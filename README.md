# Khathax (Android, Kotlin + Jetpack Compose + Firebase)

## সেটআপ
1. Android Studio-তে `Khathax` ফোল্ডার Open করুন।
2. https://console.firebase.google.com এ নতুন প্রজেক্ট বানান।
3. প্রজেক্টে Android অ্যাপ যোগ করুন, package name: `com.khathax.app`
4. `google-services.json` ডাউনলোড করে `app/` ফোল্ডারে রাখুন।
5. Firebase Console > Authentication > Sign-in method > **Email/Password** চালু করুন।
6. Firebase Console > Firestore Database > Create database।
7. Firestore > Rules এ নিচের রুলস বসিয়ে Publish করুন:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == uid;
    }
    match /chats/{chatId}/messages/{msgId} {
      allow read, create: if request.auth != null
        && chatId.matches('.*' + request.auth.uid + '.*');
    }
  }
}
```
8. Gradle Sync করে ফোনে বা এমুলেটরে Run করুন। দুটি আলাদা ডিভাইসে দুটি অ্যাকাউন্ট খুলে চ্যাট টেস্ট করুন।

## এখন যা আছে
ইমেইল সাইনআপ/লগইন, ইউজার লিস্ট, রিয়েলটাইম ১-টু-১ চ্যাট।

## পরবর্তী ধাপ (ইচ্ছা হলে)
ফোন নম্বর লগইন, ছবি/ভিডিও পাঠানো (Firebase Storage), পুশ নোটিফিকেশন (FCM), গ্রুপ চ্যাট, সিন/টাইপিং স্ট্যাটাস।
