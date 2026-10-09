import json
import os

env_data = {
    "id": "dynamicmart-local-env",
    "name": "DynamicMart local",
    "values": [
        {"key": "gateway_url", "value": "http://localhost:8080", "type": "default", "enabled": True},
        {"key": "payment_url", "value": "http://localhost:8085", "type": "default", "enabled": True},
        {"key": "internal_key", "value": "dynamicmart-local-internal-key-2026", "type": "secret", "enabled": True},
        {"key": "customer_token", "value": "", "type": "secret", "enabled": True},
        {"key": "admin_token", "value": "", "type": "secret", "enabled": True},
        {"key": "customer_id", "value": "00000000-0000-4000-8000-000000000002", "type": "default", "enabled": True},
        {"key": "province_id", "value": "201", "type": "default", "enabled": True},
        {"key": "ward_id", "value": "11007", "type": "default", "enabled": True},
        {"key": "quote_id", "value": "", "type": "default", "enabled": True},
        {"key": "quote_fingerprint", "value": "", "type": "default", "enabled": True},
        {"key": "order_id", "value": "", "type": "default", "enabled": True},
        {"key": "correlation_id", "value": "", "type": "default", "enabled": True},
        {"key": "payment_id", "value": "", "type": "default", "enabled": True},
        {"key": "payment_reference", "value": "", "type": "default", "enabled": True}
    ],
    "_postman_variable_scope": "environment"
}

with open("e:/dynamicmart/postman/DynamicMart_Local.postman_environment.json", "w", encoding="utf-8") as f:
    json.dump(env_data, f, indent=2, ensure_ascii=False)

print("Created DynamicMart_Local.postman_environment.json")
