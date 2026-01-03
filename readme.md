<p align="center">
<picture>
  <img src="/app/src/main/ic_launcher-playstore.png" alt="App Icon" width="40%" >
</picture>
</p>

# ContactGrouper

## Overview

**ContactGrouper** is an Android app that lets you organize your contacts into custom groups and assign unique ringtones to each group. When someone from a group calls you, their group's ringtone will play, allowing you to instantly recognize who's calling based on the sound.

### Key Features

- 📱 **Organize Contacts**: Create custom contact groups to organize your contacts by category (Family, Work, Friends, etc.)
- 🔔 **Custom Ringtones**: Assign a unique ringtone to each group for instant caller recognition
- 👥 **Easy Management**: Add, remove, and reassign contacts to groups with a simple UI
- 🎨 **Visual Groups**: Each group is displayed with a unique color for quick identification
- 🔄 **Real-time Updates**: Changes to group ringtones and memberships are applied immediately

---

## How to Use

### Creating a Group

1. Navigate to the **Groups** tab
2. Tap the **+ (Add)** button in the bottom right corner
3. Enter a **Group Name** (required)
4. **(Optional)** Tap **"Assign Ringtone"** to select a custom ringtone for the group
   - Choose from your device's available ringtones
   - Tap **"Change Ringtone"** if you want to change it later
5. Tap **"Save"** to create the group

### Assigning Contacts to a Group

#### Method 1: From the Contacts Tab
1. Navigate to the **Contacts** tab
2. Long-press on a contact to enter selection mode
3. Select one or more contacts
4. Tap the **"Assign to Group"** button that appears
5. Select the group you want to assign them to
6. Tap **"Assign"**

#### Method 2: From the Contact Detail
1. Navigate to the **Contacts** tab
2. Tap on a contact to view their details
3. Scroll down to the **Group** section
4. If the contact is already in a group, you can tap the delete button to remove them
5. To assign a contact to a group, use **Method 1** above

### Viewing Group Details

1. Navigate to the **Groups** tab
2. Tap on a group card to view its details
3. You'll see:
   - **Group name** with its color
   - **Member count** showing how many contacts are in the group
   - **Ringtone** currently assigned to the group
   - **List of all members** in the group

### Changing a Group's Ringtone

1. Navigate to the **Groups** tab
2. Tap on a group to view its details
3. Tap the **⋮ (Settings menu)** button in the top right
4. Select **"Change Ringtone"**
5. Choose a new ringtone from the system ringtone picker
6. The new ringtone is **applied immediately** to all contacts in the group

### Removing a Contact from a Group

#### Method 1: From Contact Details
1. Navigate to a **Contact** and tap to view details
2. Find the **Group** section at the bottom
3. Tap the **delete icon** next to the group name
4. The contact is removed from the group and reverts to the default ringtone

#### Method 2: From Group Details
1. Navigate to the **Groups** tab
2. Tap on a group to view its members
3. Find the contact you want to remove in the member list
4. Tap the **delete icon** next to their name
5. The contact is removed from the group

### Deleting a Group

1. Navigate to the **Groups** tab
2. Tap on the group you want to delete
3. Tap the **⋮ (Settings menu)** button in the top right
4. Select **"Delete Group"**
5. The group is deleted (contacts are not deleted, just unassigned)

---

## How Ringtones Work

- When a contact is assigned to a group, the group's ringtone is **automatically applied** to that contact
- Each contact can only be in **one group at a time**
- If you move a contact to a different group, the old ringtone is cleared and the new group's ringtone is applied
- If you change a group's ringtone, **all contacts in that group** automatically get the new ringtone
- If you remove a contact from a group, the **custom ringtone is cleared** and the contact reverts to the default system ringtone

---

## Technical Details

- **Built with**: Android Jetpack Compose, Room Database, Kotlin
- **Requires**: Android 6.0 or higher
- **Permissions**: Contacts (READ/WRITE), Phone (to set ringtones)

---

## Tips & Tricks

✨ **Pro Tips:**

- Use distinct ringtones for different groups (e.g., upbeat for friends, professional tone for work)
- Create a "VIP" group for important contacts and assign a unique ringtone
- Change group ringtones seasonally or by occasion
- The color of each group is randomly generated - no need to worry about duplicates!

---

## Future Enhancements

Potential features for future versions:
- Custom vibration patterns per group
- Notification LED colors per group
- Contact photos in group detail view
- Group messaging (send SMS to all members)
- Smart groups based on contact frequency

---

## Support & Feedback

If you encounter any issues or have suggestions for improvements, please feel free to reach out!

**Enjoy organizing your contacts!** 📞✨
