### What is RedeemXRestApi?
This is a addon Plugin for RedeemCodeX
It allow user generate redeemcode of minecraft through rest api with basic config

### POST-METHOD
```baseUrl/generateCode```
  ## Parameter
  - token (Required)
  - digit
  - amount
  - target
  - targetUUID
  - template

### Response Sample
```
{
    "status": "ok",
    "redeemCode": "[]"
}
```

### BaseUrl sample
```https://<host>:<port>/<method>```
