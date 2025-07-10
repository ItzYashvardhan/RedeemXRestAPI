# RedeemXRestAPI

A RESTful API layer for the [RedeemCodeX](https://builtbybit.com/resources/redeemcodex.68885/) plugin. This allows external applications or services to manage redeem codes and templates over HTTP.

## ⚙ Configuration

- Define the API `host` and `port` inside the plugin's `config.yml`.
- All endpoints require a valid token set via the `Authorization` header.

## 📦 Endpoints

### ✅ Generate Code
- **Route:** `/api/rcx/generate/code`
- **Method:** `POST`
- **Parameters (body):**
    - `digit` (Int) – Optional: Number of random codes to generate
    - `custom` (String) – Optional: Space-separated custom codes
    - `amount` (Int) – Optional: Number of times each code can be redeemed (default: `1`)
    - `template` (String) – Optional: Template name (default: `DEFAULT`)

### 🆕 Generate Template
- **Route:** `/api/rcx/generate/template`
- **Method:** `POST`
- **Parameters (body):**
    - `template` (String) – Name of the template to create

### ❌ Delete Code
- **Route:** `/api/rcx/delete/code`
- **Method:** `POST`
- **Parameters (body):**
    - `code` (String) – Space-separated list of codes to delete
    - `template` (String) – Optional: Delete all codes under given template(s)

### ❌ Delete Template
- **Route:** `/api/rcx/delete/code`
- **Method:** `POST`
- **Parameters (body):**
    - `template` (String) – Template name(s) to delete all associated codes

## 🔐 Authentication

All endpoints require an `Authorization` header:
