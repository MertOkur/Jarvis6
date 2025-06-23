package com.example.jarvis6;

import android.content.Context;

import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

class ContactsHelper {
    Context context;

    public ContactsHelper(Context context) {
        this.context = context;
    }

    public interface ContactCallback {
        void onContactsLoaded(List<Contact> contacts);
    }

    private List<Contact> getContacts() {
        var contacts = new ArrayList<Contact>();


        var contentResolver = context.getContentResolver();
        var uri = ContactsContract.Contacts.CONTENT_URI;
        var projection = new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        };
        var selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, selection, null, ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    int idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    int nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    int hasPhoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

                    if (idColumnIndex != -1 && nameColumnIndex != -1 && hasPhoneNumberColumnIndex != -1) {
                        var contactId = cursor.getString(idColumnIndex);
                        var contactName = cursor.getString(nameColumnIndex);
                        var hasPhoneNumber = cursor.getString(hasPhoneNumberColumnIndex).equals("1");

                        var phoneNumbers = new ArrayList<String>();
                        var emailAdresses = new ArrayList<String>();
                        var postAdressList = new ArrayList<String>();


                        if (hasPhoneNumber) {
                            var phoneCursor = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    new String[]{contactId},
                                    null
                            );

                            if (phoneCursor != null) {
                                while (phoneCursor.moveToNext()) {
                                    int phoneNumberColumnIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                    if (phoneNumberColumnIndex != -1) {
                                        phoneNumbers.add(phoneCursor.getString(phoneNumberColumnIndex));
                                    }
                                }
                                phoneCursor.close();
                            }
                        }

                        // --- Email Adressen abfragen ---
                        var emailCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS},
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                new String[]{contactId},
                                null
                        );
                        if (emailCursor != null) {
                            while (emailCursor.moveToNext()) {
                                int emailAdressColumnIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                                if (emailAdressColumnIndex != -1) {
                                    emailAdresses.add(emailCursor.getString(emailAdressColumnIndex));
                                }
                            }
                            emailCursor.close();
                        }

                        // --- Postadressen abfagen ---
                        var adressCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS},
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ?",
                                new String[]{contactId},
                                null
                        );

                        if (adressCursor != null) {
                            while (adressCursor.moveToNext()) {
                                int adressColumnIndex = adressCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
                                if (adressColumnIndex != -1) {
                                    postAdressList.add(adressCursor.getString(adressColumnIndex));
                                }
                            }
                            adressCursor.close();
                        }

                        contacts.add(new Contact(contactId, contactName, phoneNumbers, emailAdresses, postAdressList));
                    }
                }
            } else {
                System.out.println("Keine Kontakte gefunden.");
            }

            cursor.close();
        } catch (Exception e) {
            System.err.println("following error occurred while reading contacts: " + e);
        }

        return contacts;
    }

    public void loadContacts(ContactCallback callback) {
        var executor = Executors.newSingleThreadExecutor();
        var handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            var contactList = getContacts();

            handler.post(() -> {
                callback.onContactsLoaded(contactList);
            });
        });
    }
}
