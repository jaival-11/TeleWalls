package me.jaival.telewalls.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

sealed class MarkdownBlock {
    data class Header(val level: Int, val content: String) : MarkdownBlock()
    data class ListItem(
        val isOrdered: Boolean,
        val prefix: String,
        val content: String,
        val indentLevel: Int
    ) : MarkdownBlock()
    data class BlockQuote(val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onLinkClick: ((String) -> Unit)? = null
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val codeBgColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    if (index > 0) Spacer(modifier = Modifier.height(6.dp))
                    val (fontSize, fontWeight, headerColor) = when (block.level) {
                        1 -> Triple(16.sp, FontWeight.Bold, primaryColor)
                        2 -> Triple(14.sp, FontWeight.Bold, color)
                        else -> Triple(13.sp, FontWeight.SemiBold, color)
                    }
                    Text(
                        text = buildAnnotatedString {
                            appendMarkdownInline(
                                text = block.content,
                                primaryColor = primaryColor,
                                codeBgColor = codeBgColor,
                                onLinkClick = onLinkClick
                            )
                        },
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = headerColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = (block.indentLevel * 12).dp,
                                top = 1.5.dp,
                                bottom = 1.5.dp
                            ),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.prefix,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = buildAnnotatedString {
                                appendMarkdownInline(
                                    text = block.content,
                                    primaryColor = primaryColor,
                                    codeBgColor = codeBgColor,
                                    onLinkClick = onLinkClick
                                )
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = color
                        )
                    }
                }

                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(primaryColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                appendMarkdownInline(
                                    text = block.content,
                                    primaryColor = primaryColor,
                                    codeBgColor = codeBgColor,
                                    onLinkClick = onLinkClick
                                )
                            },
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedString {
                            appendMarkdownInline(
                                text = block.content,
                                primaryColor = primaryColor,
                                codeBgColor = codeBgColor,
                                onLinkClick = onLinkClick
                            )
                        },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = color,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        if (trimmed.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$"))) {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        if (trimmed.startsWith("#")) {
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            if (level in 1..6 && (level >= trimmed.length || trimmed[level] == ' ')) {
                val content = trimmed.substring(level).trim()
                blocks.add(MarkdownBlock.Header(level, content))
                i++
                continue
            }
        }

        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                val qLine = lines[i].trim().removePrefix(">").trim()
                quoteLines.add(qLine)
                i++
            }
            blocks.add(MarkdownBlock.BlockQuote(quoteLines.joinToString("\n")))
            continue
        }

        val unorderedMatch = Regex("^(\\s*)([*+-])\\s+(.*)$").find(line)
        val orderedMatch = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$").find(line)

        if (unorderedMatch != null) {
            val indentSpaces = unorderedMatch.groupValues[1].length
            val content = unorderedMatch.groupValues[3]
            blocks.add(
                MarkdownBlock.ListItem(
                    isOrdered = false,
                    prefix = "•",
                    content = content,
                    indentLevel = indentSpaces / 2
                )
            )
            i++
            continue
        } else if (orderedMatch != null) {
            val indentSpaces = orderedMatch.groupValues[1].length
            val number = orderedMatch.groupValues[2]
            val content = orderedMatch.groupValues[3]
            blocks.add(
                MarkdownBlock.ListItem(
                    isOrdered = true,
                    prefix = "$number.",
                    content = content,
                    indentLevel = indentSpaces / 2
                )
            )
            i++
            continue
        }

        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val pLine = lines[i]
            val pTrimmed = pLine.trim()
            if (pTrimmed.isEmpty() ||
                pTrimmed.startsWith("```") ||
                pTrimmed.matches(Regex("^(\\*{3,}|-{3,}|_{3,})$")) ||
                pTrimmed.startsWith("#") ||
                pTrimmed.startsWith(">") ||
                Regex("^(\\s*)([*+-]|\\d+\\.)\\s+").containsMatchIn(pLine)
            ) {
                break
            }
            paragraphLines.add(pTrimmed)
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
        }
    }

    return blocks
}

fun AnnotatedString.Builder.appendMarkdownInline(
    text: String,
    primaryColor: Color,
    codeBgColor: Color,
    onLinkClick: ((String) -> Unit)? = null
) {
    val pattern = Pattern.compile(
        "\\[([^\\]]+)\\]\\(([^)]+)\\)|`([^`]+)`|(\\*{2}|__)(.*?)\\4|~~(.*?)~~|(\\*|_)(.*?)\\7"
    )
    val matcher = pattern.matcher(text)
    var currentIndex = 0

    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()

        if (start > currentIndex) {
            append(text.substring(currentIndex, start))
        }

        val linkText = matcher.group(1)
        val linkUrl = matcher.group(2)
        val codeText = matcher.group(3)
        val boldText = matcher.group(5)
        val strikeText = matcher.group(6)
        val italicText = matcher.group(8)

        when {
            linkText != null && linkUrl != null -> {
                val formattedUrl = if (!linkUrl.startsWith("http://") && !linkUrl.startsWith("https://")) {
                    "https://$linkUrl"
                } else {
                    linkUrl
                }
                val linkAnnotation = LinkAnnotation.Url(
                    url = formattedUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ),
                    linkInteractionListener = onLinkClick?.let { listener ->
                        { link ->
                            if (link is LinkAnnotation.Url) {
                                listener(link.url)
                            }
                        }
                    }
                )
                withLink(linkAnnotation) {
                    appendMarkdownInline(linkText, primaryColor, codeBgColor, onLinkClick)
                }
            }
            codeText != null -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = primaryColor,
                        background = codeBgColor,
                        fontSize = 11.5.sp
                    )
                ) {
                    append(codeText)
                }
            }
            boldText != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendMarkdownInline(boldText, primaryColor, codeBgColor, onLinkClick)
                }
            }
            strikeText != null -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendMarkdownInline(strikeText, primaryColor, codeBgColor, onLinkClick)
                }
            }
            italicText != null -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendMarkdownInline(italicText, primaryColor, codeBgColor, onLinkClick)
                }
            }
            else -> {
                append(matcher.group(0) ?: "")
            }
        }

        currentIndex = end
    }

    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
