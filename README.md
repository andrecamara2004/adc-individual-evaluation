# ADC 2025/2026 — Individual Evaluation: Demo Guide

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

### 2.4 Test Locally (Postman)

All endpoints use `POST` with `Content-Type: application/json`.
Base URL: `http://localhost:8080/rest/`

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

## 4. Testing All 10 Operations

**Important:** Start with empty databases (no accounts, no tokens).

### Op1 — Create Accounts

**POST** `/rest/createaccount`

Create ADMIN:
```json
{
  "input": {
    "username": "admin@adc.pt",
    "password": "admin123",
    "confirmation": "admin123",
    "phone": "912345678",
    "address": "Lisboa",
    "role": "ADMIN"
  }
}
```

Create USER:
```json
{
  "input": {
    "username": "user@adc.pt",
    "password": "user123",
    "confirmation": "user123",
    "phone": "919876543",
    "address": "Porto",
    "role": "USER"
  }
}
```

Create BOFFICER:
```json
{
  "input": {
    "username": "officer@adc.pt",
    "password": "officer123",
    "confirmation": "officer123",
    "phone": "913456789",
    "address": "Faro",
    "role": "BOFFICER"
  }
}
```

### Op2 — Login

**POST** `/rest/login`

Login as ADMIN (save the token for subsequent tests):
```json
{
  "input": {
    "username": "admin@adc.pt",
    "password": "admin123"
  }
}
```

Login as USER:
```json
{
  "input": {
    "username": "user@adc.pt",
    "password": "user123"
  }
}
```

### Op3 — Show Users

**POST** `/rest/showusers` (Requires ADMIN or BOFFICER token)

```json
{
  "input": {},
  "token": { <PASTE ADMIN TOKEN HERE> }
}
```

### Op4 — Delete Account

**POST** `/rest/deleteaccount` (Requires ADMIN token)

```json
{
  "input": {
    "username": "officer@adc.pt"
  },
  "token": { <PASTE ADMIN TOKEN HERE> }
}
```

Verify with Op3 (ShowUsers) that the account no longer appears.

### Op5 — Modify Account Attributes

**POST** `/rest/modaccount` (ADMIN: any account, BOFFICER: own + USER, USER: own only)

```json
{
  "input": {
    "username": "user@adc.pt",
    "attributes": {
      "phone": "999999999",
      "address": "Coimbra"
    }
  },
  "token": { <PASTE ADMIN TOKEN HERE> }
}
```

### Op6 — Show Authenticated Sessions

**POST** `/rest/showauthsessions` (Requires ADMIN token)

```json
{
  "input": {},
  "token": { <PASTE ADMIN TOKEN HERE> }
}
```

### Op7 — Show User Role

**POST** `/rest/showuserrole` (Requires ADMIN or BOFFICER token)

```json
{
  "input": {
    "username": "user@adc.pt"
  },
  "token": { <PASTE ADMIN TOKEN HERE> }
}
```

### Op8 — Change User Role

**POST** `/rest/changeuserrole` (Requires ADMIN token)

```json
{
  "input": {
    "username": "user@adc.pt",
    "newRole": "BOFFICER"
  },
  "token": { <PASTE ADMIN TOKEN HERE> }
}
```

Verify with Op7 that the role changed.

### Op9 — Change User Password

**POST** `/rest/changeuserpwd` (Any role, own password only)

Login as user@adc.pt first, then use that token:
```json
{
  "input": {
    "username": "user@adc.pt",
    "oldPassword": "user123",
    "newPassword": "newpass456"
  },
  "token": { <PASTE USER TOKEN HERE> }
}
```

Verify: login with old password should fail (9900), login with new password should succeed.

### Op10 — Logout

**POST** `/rest/logout` (USER/BOFFICER: own session, ADMIN: any session)

```json
{
  "input": {
    "username": "user@adc.pt"
  },
  "token": { <PASTE USER TOKEN HERE> }
}
```

Verify: using the same token again should return 9903 (INVALID_TOKEN).

---

## 5. Error Codes Reference

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

## 6. Role-Based Access Control (RBAC)

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

## 7. Clean Up (Before Demo / Submission)

### Clear Cloud Datastore

Go to: https://console.cloud.google.com/datastore/entities?project=my-first-adc-project

Select and delete all entities from kinds: `User`, `Token`, `UserLog`.

### Clear Local Emulator Data

```bash
rm -rf ~/.config/gcloud/emulators/datastore
```

---

## 8. Tech Stack

- **Runtime:** Java 21 on Google App Engine (Standard)
- **Framework:** Jersey (Jakarta EE) with Gson
- **Database:** Google Firestore in Datastore mode
- **Password Hashing:** SHA-512 (Apache Commons Codec)
- **Token Validity:** 2 hours
