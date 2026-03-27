# ADC 2025/2026 — Individual Evaluation

## Project Info

- **Student:** André Câmara
- **GitHub Repository:** https://github.com/andrecamara2004/adc-individual-evaluation
- **Cloud URL:** https://my-first-adc-project.ew.r.appspot.com
- **GCP Project ID:** my-first-adc-project

---

## 1. Clone the Repository

```bash
git clone https://github.com/andrecamara2004/adc-individual-evaluation.git
cd adc-individual-evaluation
```

---

## 2. Local Deployment

### 2.1 Start the Datastore Emulator

Open a terminal and run:

```bash
gcloud beta emulators datastore start
```

Leave this terminal running.

### 2.2 Set Environment Variables

Open a **new terminal** and run:

```bash
$(gcloud beta emulators datastore env-init)
export DATASTORE_USE_PROJECT_ID_AS_APP_ID=true
```

### 2.3 Build and Run Locally

In the same terminal(wsl):

```bash
mvn clean package appengine:run -Dapp.devserver.host=0.0.0.0
```

Not wsl:
```bash
mvn clean package appengine:run
```

> **Note:** The `-Dapp.devserver.host=0.0.0.0` flag is needed when running inside WSL so that the server is accessible from the Windows host (e.g., Postman on Windows).

Wait until you see `Dev App Server is now running`. The app will be available at:

```
http://localhost:8080
```

---

## 3. Cloud Deployment

### 3.1 Configure GCP Project

```bash
gcloud config set project my-first-adc-project
gcloud auth login
```

### 3.2 Deploy

```bash
mvn clean package appengine:deploy -Dapp.deploy.projectId=my-first-adc-project -Dapp.deploy.version=1
```

The app will be available at:

```
https://my-first-adc-project.ew.r.appspot.com
```

### 3.3 Test on Cloud (Postman)

Base URL: `https://my-first-adc-project.ew.r.appspot.com/rest/`

---

## Error Codes Reference

| Code | Error | Description |
|------|-------|-------------|
| 9900 | INVALID_CREDENTIALS | Wrong username/password |
| 9901 | USER_ALREADY_EXISTS | Username already taken |
| 9902 | USER_NOT_FOUND | Username not found |
| 9903 | INVALID_TOKEN | Token invalid or not found |
| 9904 | TOKEN_EXPIRED | Token has expired |
| 9905 | UNAUTHORIZED | Role not allowed for this operation |
| 9906 | INVALID_INPUT | Input data doesn't follow spec |
| 9907 | FORBIDDEN | Other forbidden error |

---

## Role-Based Access Control (RBAC)

| Operation | USER | BOFFICER | ADMIN |
|-----------|------|----------|-------|
| Op1 CreateAccount | ✅ | ✅ | ✅ |
| Op2 Login | ✅ | ✅ | ✅ |
| Op3 ShowUsers | ❌ | ✅ | ✅ |
| Op4 DeleteAccount | ❌ | ❌ | ✅ |
| Op5 ModifyAccount | Own only | Own + USER | Any |
| Op6 ShowAuthSessions | ❌ | ❌ | ✅ |
| Op7 ShowUserRole | ❌ | ✅ | ✅ |
| Op8 ChangeUserRole | ❌ | ❌ | ✅ |
| Op9 ChangeUserPwd | Own only | Own only | Own only |
| Op10 Logout | Own only | Own only | Any |

---

## Clean Up (Before Demo / Submission)

### Clear Cloud Datastore

Go to: https://console.cloud.google.com/datastore/entities?project=my-first-adc-project

Select and delete all entities from kinds: `User`, `Token`, `UserLog`.

### Clear Local Emulator Data

```bash
rm -rf ~/.config/gcloud/emulators/datastore
```

---

## Tech Stack

- **Runtime:** Java 21 on Google App Engine (Standard)
- **Framework:** Jersey
- **Database:** Google Firestore in Datastore mode
- token expires after 15 minutes
