package me.jaival.telewalls.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterAuthorUtilsTest {

    @Test
    fun testCharacterListIsNotEmpty() {
        assertFalse(CharacterAuthorUtils.ALL_TV_CHARACTERS.isEmpty())
    }

    @Test
    fun testContainsCharactersFromAllRequiredShows() {
        // The Office
        assertTrue(CharacterAuthorUtils.OFFICE_CHARACTERS.contains("Michael Scott"))
        assertTrue(CharacterAuthorUtils.OFFICE_CHARACTERS.contains("Dwight Schrute"))

        // FRIENDS
        assertTrue(CharacterAuthorUtils.FRIENDS_CHARACTERS.contains("Chandler Bing"))
        assertTrue(CharacterAuthorUtils.FRIENDS_CHARACTERS.contains("Rachel Green"))

        // PSYCH
        assertTrue(CharacterAuthorUtils.PSYCH_CHARACTERS.contains("Shawn Spencer"))
        assertTrue(CharacterAuthorUtils.PSYCH_CHARACTERS.contains("Burton Guster"))

        // Brooklyn Nine-Nine
        assertTrue(CharacterAuthorUtils.BROOKLYN_NINE_NINE_CHARACTERS.contains("Jake Peralta"))
        assertTrue(CharacterAuthorUtils.BROOKLYN_NINE_NINE_CHARACTERS.contains("Amy Santiago"))
    }

    @Test
    fun testGetRandomCharacterNameReturnsValidCharacter() {
        val randomName = CharacterAuthorUtils.getRandomCharacterName()
        assertTrue(CharacterAuthorUtils.ALL_TV_CHARACTERS.contains(randomName))
    }
}
