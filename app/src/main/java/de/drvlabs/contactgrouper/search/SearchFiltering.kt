package de.drvlabs.contactgrouper.search

import de.drvlabs.contactgrouper.contacts.Contact
import de.drvlabs.contactgrouper.groups.Group
import java.text.Normalizer
import java.util.Locale

fun filterContactsBySearchQuery(
    contacts: List<Contact>,
    groups: List<Group>,
    query: String
): List<Contact> {
    val searchTerms = query.searchTerms()
    if (searchTerms.isEmpty()) {
        return contacts
    }

    val groupsById = groups.associateBy(Group::id)
    return contacts.filter { contact ->
        val groupNames = contact.groupIds.mapNotNull { groupId -> groupsById[groupId]?.name }
        contact.searchIndex(groupNames).matchesAll(searchTerms)
    }
}

fun filterGroupsBySearchQuery(
    groups: List<Group>,
    contacts: List<Contact>,
    query: String
): List<Group> {
    val searchTerms = query.searchTerms()
    if (searchTerms.isEmpty()) {
        return groups
    }

    val contactsByGroupId = buildMap<Int, MutableList<Contact>> {
        contacts.forEach { contact ->
            contact.groupIds.forEach { groupId ->
                getOrPut(groupId) { mutableListOf() }.add(contact)
            }
        }
    }

    return groups.filter { group ->
        group.searchIndex(contactsByGroupId[group.id].orEmpty()).matchesAll(searchTerms)
    }
}

private fun Contact.searchIndex(groupNames: List<String>): List<String> {
    return buildSearchIndex(
        buildList {
            add(displayName)
            nickname?.let(::add)
            addAll(phoneNumbers.map { it.value })
            addAll(emails.map { it.value })
            addAll(groupNames)
        }
    )
}

private fun Group.searchIndex(members: List<Contact>): List<String> {
    return buildSearchIndex(
        buildList {
            add(name)
            members.forEach { contact ->
                add(contact.displayName)
                contact.nickname?.let(::add)
            }
        }
    )
}

private fun List<String>.matchesAll(searchTerms: List<String>): Boolean {
    return searchTerms.all { searchTerm ->
        any { indexedValue -> indexedValue.contains(searchTerm) }
    }
}

private fun buildSearchIndex(values: List<String>): List<String> {
    return values.flatMap { value ->
        val normalized = value.normalizeForSearch()
        val compact = normalized.filterNot(Char::isWhitespace)
        if (compact == normalized) {
            listOf(normalized)
        } else {
            listOf(normalized, compact)
        }
    }
}

private fun String.searchTerms(): List<String> {
    return normalizeForSearch()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
}

private fun String.normalizeForSearch(): String {
    val withoutAccents = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutAccents
        .lowercase(Locale.ROOT)
        .map { character ->
            when {
                character.isLetterOrDigit() -> character
                character.isWhitespace() -> ' '
                else -> ' '
            }
        }
        .joinToString(separator = "")
        .trim()
        .replace(Regex("\\s+"), " ")
}
