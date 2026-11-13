# SayWhat?

SayWhat? is an Android memory game in which players repeat growing button sequences. It includes single-player difficulty levels and leaderboards, plus multiplayer lobbies and matches.

## Portfolio notice

Copyright © 2025 Luís Costa, Andrei Panescu, Maria Ciurila. All rights reserved.

This project was created as a course project at the University of A Coruña (UDC). It is not affiliated with or endorsed by the university.

This project is shared as a portfolio showcase of a university course project. It is provided for viewing and is not intended for production use or redistribution. No license is granted for the project code; third-party components remain subject to their own licenses and rights. This notice is subject to applicable law and the hosting platform’s terms.

The project is provided as is, without warranties. To the extent permitted by applicable law, the author disclaims liability for damages arising from its use or misuse. Anyone using the project is responsible for their own use.

## Built with

- Java and Android Views
- Firebase Authentication and Cloud Firestore
- Gradle (Kotlin DSL)

## Run locally

1. Open the project in Android Studio with the Android SDK for API 36 installed. The app requires Android 10 (API 29) or later.
2. Create your own Firebase project and register an Android app with the package name `es.udc.psi.saywhat`. Download its `google-services.json` into `app/`.
3. Enable Cloud Firestore and, if you want Google sign-in, Firebase Authentication with the Google provider. Configure the Android app's signing certificate fingerprints in Firebase for Google sign-in.
4. Configure and test Firestore security rules for your use case before using real data. This repository does not provide or deploy rules; the app uses `users` and `games` collections, including guest profiles.
5. Sync Gradle and run the app from Android Studio, or use `sh gradlew assembleDebug`.

Firebase configuration and release signing files are intentionally excluded from Git. Do not commit credentials or signing keys. This project is provided as a portfolio example; review the security rules and access model before deploying it for real users.

## Credits and third-party material

- **Font:** [Carter One](https://fonts.google.com/specimen/Carter+One), Copyright (c) 2011 by Vernon Adams, licensed under the [SIL Open Font License 1.1](https://openfontlicense.org) ([full license text](LICENSES/OFL-CarterOne.md)).
- **Libraries:** Firebase, FirebaseUI and AndroidX, used as Gradle dependencies under their own licenses (primarily Apache 2.0). They are not redistributed in this repository.

Third-party assets remain the property of their respective owners.
