#!/usr/bin/env python3
import os
import sys
import asyncio
import urllib.request
import json

from telethon import TelegramClient
from telethon.sessions import StringSession
from telethon.tl.types import InputPeerChannel, InputPeerChat

def send_text_via_bot_api_fallback(bot_token, chat_id, topic_id, text):
    """Fallback method to send text message via Telegram Bot API HTTP."""
    print("Attempting fallback text message via Telegram Bot API HTTP...", flush=True)
    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"

    payload = {
        "chat_id": chat_id,
        "text": text,
        "parse_mode": "HTML",
        "disable_web_page_preview": False
    }
    if topic_id:
        try:
            payload["message_thread_id"] = int(topic_id)
        except ValueError:
            payload["message_thread_id"] = topic_id

    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode('utf-8'),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            res_data = json.loads(resp.read().decode('utf-8'))
            if res_data.get("ok"):
                print("Fallback text message via Bot API succeeded!", flush=True)
                return True
    except Exception as e:
        print(f"Fallback HTML text message error: {e}", flush=True)

    # Second attempt: Plain text fallback (no parse_mode) if HTML fails
    print("Attempting plain text message via Bot API HTTP...", flush=True)
    payload.pop("parse_mode", None)
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode('utf-8'),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            res_data = json.loads(resp.read().decode('utf-8'))
            if res_data.get("ok"):
                print("Fallback plain text message via Bot API succeeded!", flush=True)
                return True
            else:
                print(f"Fallback plain text message failed: {res_data}", flush=True)
                return False
    except Exception as e:
        print(f"Fallback plain text message error: {e}", flush=True)
        return False

def send_via_bot_api_fallback(bot_token, chat_id, topic_id, apk_path):
    """Fallback to upload APK using Telegram Bot API HTTP multipart request."""
    print("Attempting fallback APK upload via Telegram Bot API HTTP...", flush=True)
    url = f"https://api.telegram.org/bot{bot_token}/sendDocument"
    filename = os.path.basename(apk_path)
    boundary = "----GitHubActionsMTProtoFallback"

    lines = []

    # chat_id
    lines.append(f"--{boundary}".encode('utf-8'))
    lines.append(f'Content-Disposition: form-data; name="chat_id"'.encode('utf-8'))
    lines.append(b"")
    lines.append(str(chat_id).encode('utf-8'))

    # message_thread_id
    if topic_id:
        lines.append(f"--{boundary}".encode('utf-8'))
        lines.append(f'Content-Disposition: form-data; name="message_thread_id"'.encode('utf-8'))
        lines.append(b"")
        lines.append(str(topic_id).encode('utf-8'))

    # document
    lines.append(f"--{boundary}".encode('utf-8'))
    lines.append(f'Content-Disposition: form-data; name="document"; filename="{filename}"'.encode('utf-8'))
    lines.append(b"Content-Type: application/vnd.android.package-archive")
    lines.append(b"")

    with open(apk_path, "rb") as f:
        file_bytes = f.read()

    body = b"\r\n".join(lines) + b"\r\n" + file_bytes + b"\r\n" + f"--{boundary}--\r\n".encode('utf-8')

    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            res_data = json.loads(resp.read().decode('utf-8'))
            if res_data.get("ok"):
                print("Fallback upload via Bot API succeeded!", flush=True)
                return True
            else:
                print(f"Fallback upload failed: {res_data}", flush=True)
                return False
    except Exception as e:
        print(f"Fallback upload error: {e}", flush=True)
        return False

async def get_target_entity(client, target_chat):
    try:
        return await client.get_input_entity(target_chat)
    except Exception:
        pass
    try:
        return await client.get_entity(target_chat)
    except Exception:
        pass

    chat_str = str(target_chat).strip()
    if chat_str.startswith("-100"):
        try:
            channel_id = int(chat_str[4:])
            return InputPeerChannel(channel_id=channel_id, access_hash=0)
        except Exception:
            pass
    elif chat_str.startswith("-"):
        try:
            group_id = int(chat_str[1:])
            return InputPeerChat(chat_id=group_id)
        except Exception:
            pass

    return target_chat

async def send_mtproto():
    api_id_val = (
        os.environ.get("TELEGRAM_API_ID")
        or os.environ.get("TELEGRAM_API_KEY")
        or os.environ.get("API_ID")
        or os.environ.get("API_KEY")
    )
    api_hash = (
        os.environ.get("TELEGRAM_API_HASH")
        or os.environ.get("API_HASH")
    )
    bot_token = (
        os.environ.get("TELEGRAM_BOT_TOKEN")
        or os.environ.get("BOT_TOKEN")
    )
    session_string = (
        os.environ.get("TELEGRAM_STRING_SESSION")
        or os.environ.get("TELEGRAM_SESSION")
        or os.environ.get("STRING_SESSION")
        or os.environ.get("SESSION")
    )

    chat_id = os.environ.get("CHAT_ID")
    topic_id = os.environ.get("TOPIC_ID")
    text = os.environ.get("TEXT")
    apk_path = os.environ.get("APK_PATH")

    print(f"=== Telegram CI Notification Handler ===", flush=True)
    print(f"Target Chat ID: {chat_id}", flush=True)
    print(f"Target Topic ID: {topic_id if topic_id else '(None)'}", flush=True)
    print(f"APK File Path: {apk_path if apk_path else '(None)'}", flush=True)

    if not api_id_val or not api_hash:
        print("Error: TELEGRAM_API_ID (or TELEGRAM_API_KEY) and TELEGRAM_API_HASH secrets are required.", flush=True)
        sys.exit(1)

    try:
        api_id = int(api_id_val)
    except ValueError:
        print(f"Error: API ID '{api_id_val}' must be an integer.", flush=True)
        sys.exit(1)

    if not chat_id:
        print("Error: CHAT_ID is empty.", flush=True)
        sys.exit(1)

    try:
        target_chat = int(chat_id)
    except ValueError:
        target_chat = chat_id

    reply_to = None
    if topic_id and topic_id.strip():
        try:
            reply_to = int(topic_id.strip())
        except ValueError:
            pass

    if session_string and session_string.strip():
        print("Authenticating via TELEGRAM_STRING_SESSION (User Account)...", flush=True)
        client = TelegramClient(StringSession(session_string.strip()), api_id, api_hash)
        await client.start()
    elif bot_token and bot_token.strip():
        print("Authenticating via TELEGRAM_BOT_TOKEN (Bot Account)...", flush=True)
        client = TelegramClient(StringSession(), api_id, api_hash)
        await client.start(bot_token=bot_token.strip())
    else:
        print("Error: Either TELEGRAM_BOT_TOKEN or TELEGRAM_STRING_SESSION must be provided in secrets.", flush=True)
        sys.exit(1)

    try:
        entity = await get_target_entity(client, target_chat)
        print(f"Resolved Target Peer: {entity}", flush=True)

        # STEP 1: Send Release Notes / Notification text FIRST
        if text and text.strip():
            print(f"[1/2] Sending release notes / notification text to {target_chat} (reply_to={reply_to})...", flush=True)
            text_success = False
            try:
                await asyncio.wait_for(
                    client.send_message(
                        entity,
                        text,
                        parse_mode="html",
                        reply_to=reply_to
                    ),
                    timeout=60
                )
                print("Release notes sent successfully via MTProto HTML.", flush=True)
                text_success = True
            except Exception as e:
                print(f"Error sending HTML text via MTProto: {e}", flush=True)
                try:
                    await asyncio.wait_for(
                        client.send_message(
                            entity,
                            text,
                            parse_mode=None,
                            reply_to=reply_to
                        ),
                        timeout=60
                    )
                    print("Release notes sent successfully via MTProto Plain Text.", flush=True)
                    text_success = True
                except Exception as e2:
                    print(f"Error sending plain text via MTProto: {e2}", flush=True)

            if not text_success and bot_token and bot_token.strip():
                send_text_via_bot_api_fallback(bot_token.strip(), chat_id, topic_id, text)

        # STEP 2: Upload APK file SECOND (only after text message completes)
        if apk_path and os.path.isfile(apk_path):
            file_size_mb = os.path.getsize(apk_path) / (1024 * 1024)
            print(f"[2/2] Uploading APK file '{apk_path}' ({file_size_mb:.2f} MB) to {target_chat} (reply_to={reply_to})...", flush=True)

            last_percent = -1
            def progress(current, total):
                nonlocal last_percent
                percent = int((current / total) * 100)
                if percent // 10 > last_percent // 10:
                    last_percent = percent
                    print(f"MTProto Upload Progress: {percent}% ({current}/{total} bytes)", flush=True)

            mtproto_success = False
            try:
                await asyncio.wait_for(
                    client.send_file(
                        entity,
                        apk_path,
                        force_document=True,
                        reply_to=reply_to,
                        progress_callback=progress
                    ),
                    timeout=180
                )
                print("APK file uploaded successfully via MTProto.", flush=True)
                mtproto_success = True
            except asyncio.TimeoutError:
                print("Error: MTProto APK upload timed out after 180 seconds.", flush=True)
            except Exception as e:
                print(f"Error uploading APK file via MTProto: {e}", flush=True)

            if not mtproto_success and bot_token and bot_token.strip():
                send_via_bot_api_fallback(bot_token.strip(), chat_id, topic_id, apk_path)

    finally:
        if client and client.is_connected():
            print("Disconnecting MTProto client...", flush=True)
            try:
                await client.disconnect()
            except Exception:
                pass

if __name__ == "__main__":
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        loop.run_until_complete(send_mtproto())
    except Exception as e:
        print(f"Fatal error in send_mtproto: {e}", flush=True)
    finally:
        try:
            loop.close()
        except Exception:
            pass
        os._exit(0)
