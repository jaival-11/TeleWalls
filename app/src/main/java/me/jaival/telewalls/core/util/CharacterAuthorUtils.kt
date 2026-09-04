package me.jaival.telewalls.core.util

/**
 * Utility providing random author names based on popular/main characters
 * from The Office, FRIENDS, PSYCH, and Brooklyn Nine-Nine.
 */
object CharacterAuthorUtils {

    val OFFICE_CHARACTERS = listOf(
        "Michael Scott",
        "Dwight Schrute",
        "Jim Halpert",
        "Pam Beesly",
        "Ryan Howard",
        "Andy Bernard",
        "Angela Martin",
        "Kevin Malone",
        "Oscar Martinez",
        "Stanley Hudson",
        "Phyllis Vance",
        "Creed Bratton",
        "Meredith Palmer",
        "Kelly Kapoor",
        "Toby Flenderson",
        "Darryl Philbin",
        "Erin Hannon",
        "Jan Levinson",
        "Gabe Lewis",
        "Robert California"
    )

    val FRIENDS_CHARACTERS = listOf(
        "Chandler Bing",
        "Joey Tribbiani",
        "Ross Geller",
        "Monica Geller",
        "Rachel Green",
        "Phoebe Buffay",
        "Gunther",
        "Janice Hosenstein",
        "Mike Hannigan"
    )

    val PSYCH_CHARACTERS = listOf(
        "Shawn Spencer",
        "Burton Guster",
        "Carlton Lassiter",
        "Juliet O'Hara",
        "Chief Karen Vick",
        "Henry Spencer",
        "Buzz McNab",
        "Pierre Despereaux",
        "Woody Strode"
    )

    val BROOKLYN_NINE_NINE_CHARACTERS = listOf(
        "Jake Peralta",
        "Amy Santiago",
        "Rosa Diaz",
        "Terry Jeffords",
        "Captain Raymond Holt",
        "Charles Boyle",
        "Gina Linetti",
        "Michael Hitchcock",
        "Norm Scully",
        "Doug Judy"
    )

    val ALL_TV_CHARACTERS: List<String> = OFFICE_CHARACTERS + FRIENDS_CHARACTERS + PSYCH_CHARACTERS + BROOKLYN_NINE_NINE_CHARACTERS

    fun getRandomCharacterName(): String {
        return ALL_TV_CHARACTERS.random()
    }
}
