package de.drvlabs.contactgrouper.contacts

import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMembership
import de.drvlabs.contactgrouper.groups.RingtoneResolution

internal object ContactsStateMapper {

    fun build(
        contacts: List<Contact>,
        groups: List<Group>,
        memberships: List<GroupMembership>
    ): ContactsListState {
        val groupsById = groups.associateBy { it.id }
        val membershipsByContact = memberships.groupBy { it.contactId }

        val mappedContacts = contacts.map { contact ->
            val contactMemberships = membershipsByContact[contact.id].orEmpty()
            val orderedGroupIds = contactMemberships
                .sortedByDescending(GroupMembership::assignedAt)
                .map(GroupMembership::groupId)
                .distinct()
            val winningMembership =
                RingtoneResolution.resolveWinningMembership(groupsById, contactMemberships)

            contact.copy(
                groupIds = orderedGroupIds,
                effectiveRingtoneGroupId = winningMembership?.group?.id
            )
        }

        return ContactsListState(contacts = mappedContacts)
    }
}
