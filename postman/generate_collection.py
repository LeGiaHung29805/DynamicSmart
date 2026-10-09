import json

collection = {
    "info": {
        "name": "DynamicMart - Người 4 (Shipping & Payment)",
        "description": "Bộ Postman Collection chuẩn kiểm thử toàn bộ API Người 4: GHN Locations, GHN Quotes, Payment/COD, VNPay, ZaloPay, PayOS, VietQR/SePay, Callback Webhooks và Admin/Audit APIs.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "00. Health Check",
            "item": [
                {
                    "name": "Health Check API Gateway",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{gateway_url}}/actuator/health",
                            "host": ["{{gateway_url}}"],
                            "path": ["actuator", "health"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Gateway Status is UP (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().status).to.eql('UP');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "Health Check Payment Service",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{payment_url}}/actuator/health",
                            "host": ["{{payment_url}}"],
                            "path": ["actuator", "health"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Payment Service Status is UP (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().status).to.eql('UP');",
                                    "});"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "01. Authentication (Lấy JWT)",
            "item": [
                {
                    "name": "1.1 Login Customer",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"email\": \"customer@dynamicmart.local\",\n  \"password\": \"Password@123\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/auth/login",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "auth", "login"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Login Customer thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});",
                                    "const body = pm.response.json();",
                                    "pm.environment.set('customer_token', body.accessToken);",
                                    "pm.environment.set('customer_id', body.user.id);",
                                    "console.log('Customer Token set successfully');"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "1.2 Login Admin",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"email\": \"admin@dynamicmart.local\",\n  \"password\": \"Password@123\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/auth/login",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "auth", "login"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Login Admin thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});",
                                    "const body = pm.response.json();",
                                    "pm.environment.set('admin_token', body.accessToken);",
                                    "console.log('Admin Token set successfully');"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "02. Địa giới GHN (Mục 4)",
            "item": [
                {
                    "name": "2.1 Đồng bộ địa giới GHN (Admin)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{admin_token}}"},
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/locations/sync",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "locations", "sync"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Sync địa giới GHN thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body).to.have.property('provinceCount');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "2.2 Lấy danh sách Tỉnh/Thành phố",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/locations/provinces",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "locations", "provinces"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Lấy danh sách Tỉnh thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body).to.be.an('array').that.is.not.empty;",
                                    "    pm.environment.set('province_id', body[0].id);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "2.3 Lấy danh sách Phường/Xã theo Tỉnh",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/locations/wards?provinceId={{province_id}}",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "locations", "wards"],
                            "query": [
                                {"key": "provinceId", "value": "{{province_id}}"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Lấy danh sách Phường/Xã thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body).to.be.an('array').that.is.not.empty;",
                                    "    pm.environment.set('ward_id', body[0].id);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "2.4 Xác thực cặp Tỉnh/Phường (Hợp lệ)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/locations/validate?provinceId={{province_id}}&wardId={{ward_id}}",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "locations", "validate"],
                            "query": [
                                {"key": "provinceId", "value": "{{province_id}}"},
                                {"key": "wardId", "value": "{{ward_id}}"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Cặp Tỉnh/Phường hợp lệ (200 & valid=true)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().valid).to.eql(true);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "2.5 [Negative] Xác thực Tỉnh/Phường không khớp (422)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/locations/validate?provinceId={{province_id}}&wardId=999999",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "locations", "validate"],
                            "query": [
                                {"key": "provinceId", "value": "{{province_id}}"},
                                {"key": "wardId", "value": "999999"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Báo lỗi 422 GHN_LOCATION_MISMATCH', function () {",
                                    "    pm.response.to.have.status(422);",
                                    "    pm.expect(pm.response.json().code).to.eql('GHN_LOCATION_MISMATCH');",
                                    "});"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "03. Báo giá GHN (Mục 5)",
            "item": [
                {
                    "name": "3.1 Tạo báo giá GHN",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"customerId\": \"{{customer_id}}\",\n  \"provinceId\": {{province_id}},\n  \"wardId\": {{ward_id}},\n  \"items\": [\n    {\n      \"variantId\": \"11111111-1111-4111-8111-111111111111\",\n      \"quantity\": 2,\n      \"weightGrams\": 500,\n      \"lengthCm\": 25,\n      \"widthCm\": 20,\n      \"heightCm\": 10\n    }\n  ],\n  \"shippingDiscountVnd\": 0,\n  \"serviceCode\": null\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/shipping/quotes",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "shipping", "quotes"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Tạo báo giá thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});",
                                    "const body = pm.response.json();",
                                    "pm.expect(body).to.have.property('feeVnd');",
                                    "pm.expect(body).to.have.property('quoteId');",
                                    "pm.environment.set('quote_id', body.quoteId);",
                                    "pm.environment.set('quote_fingerprint', body.requestFingerprint);"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "3.2 Gửi lại báo giá (Kiểm tra Idempotent/Cache)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"customerId\": \"{{customer_id}}\",\n  \"provinceId\": {{province_id}},\n  \"wardId\": {{ward_id}},\n  \"items\": [\n    {\n      \"variantId\": \"11111111-1111-4111-8111-111111111111\",\n      \"quantity\": 2,\n      \"weightGrams\": 500,\n      \"lengthCm\": 25,\n      \"widthCm\": 20,\n      \"heightCm\": 10\n    }\n  ],\n  \"shippingDiscountVnd\": 0,\n  \"serviceCode\": null\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/shipping/quotes",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "shipping", "quotes"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Trả lại đúng quote còn hiệu lực không tạo mới', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().quoteId).to.eql(pm.environment.get('quote_id'));",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "3.3 Xác thực và dùng quote lần 1 (200 OK)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"customerId\": \"{{customer_id}}\",\n  \"requestFingerprint\": \"{{quote_fingerprint}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/shipping/quotes/{{quote_id}}/validate",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "shipping", "quotes", "{{quote_id}}", "validate"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Dùng quote lần đầu thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "3.4 [Negative] Xác thực và dùng quote lần 2 (409 Conflict)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"customerId\": \"{{customer_id}}\",\n  \"requestFingerprint\": \"{{quote_fingerprint}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/shipping/quotes/{{quote_id}}/validate",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "shipping", "quotes", "{{quote_id}}", "validate"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Quote chỉ dùng 1 lần, lần 2 trả 409 SHIPPING_QUOTE_INVALID', function () {",
                                    "    pm.response.to.have.status(409);",
                                    "    pm.expect(pm.response.json().code).to.eql('SHIPPING_QUOTE_INVALID');",
                                    "});"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "04. Payment Context & COD (Mục 6)",
            "item": [
                {
                    "name": "4.1 Tạo Payment COD trả sau (POSTPAID)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"orderId\": \"{{order_id}}\",\n  \"customerId\": \"{{customer_id}}\",\n  \"amountVnd\": 150000,\n  \"timing\": \"POSTPAID\",\n  \"method\": \"COD\",\n  \"correlationId\": \"{{correlation_id}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/order-context",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "order-context"]
                        }
                    },
                    "event": [
                        {
                            "listen": "prerequest",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.environment.set('order_id', pm.variables.replaceIn('{{$guid}}'));",
                                    "pm.environment.set('correlation_id', pm.variables.replaceIn('{{$guid}}'));"
                                ]
                            }
                        },
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Tạo COD thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body.status).to.eql('PENDING');",
                                    "    pm.expect(body.method).to.eql('COD');",
                                    "    pm.expect(body.redirectUrl).to.be.null;",
                                    "    pm.environment.set('payment_id', body.id);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "4.2 [Negative] Tạo COD trả trước PREPAID (422 Forbidden)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"orderId\": \"{{$guid}}\",\n  \"customerId\": \"{{customer_id}}\",\n  \"amountVnd\": 150000,\n  \"timing\": \"PREPAID\",\n  \"method\": \"COD\",\n  \"correlationId\": \"{{$guid}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/order-context",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "order-context"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('PREPAID COD bị chặn với 422 PREPAID_COD_FORBIDDEN', function () {",
                                    "    pm.response.to.have.status(422);",
                                    "    pm.expect(pm.response.json().code).to.eql('PREPAID_COD_FORBIDDEN');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "4.3 [Negative] Cùng orderId nhưng đổi amount (409 Conflict)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"orderId\": \"{{order_id}}\",\n  \"customerId\": \"{{customer_id}}\",\n  \"amountVnd\": 999999,\n  \"timing\": \"POSTPAID\",\n  \"method\": \"COD\",\n  \"correlationId\": \"{{correlation_id}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/order-context",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "order-context"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Conflict context trả về 409 ORDER_PAYMENT_CONTEXT_CONFLICT', function () {",
                                    "    pm.response.to.have.status(409);",
                                    "    pm.expect(pm.response.json().code).to.eql('ORDER_PAYMENT_CONTEXT_CONFLICT');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "4.4 Lấy Payment theo ID",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{customer_token}}"},
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/{{payment_id}}?customerId={{customer_id}}",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "{{payment_id}}"],
                            "query": [
                                {"key": "customerId", "value": "{{customer_id}}"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Lấy Payment theo ID thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().id).to.eql(pm.environment.get('payment_id'));",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "4.5 Lấy Payment theo Order ID",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/orders/{{order_id}}",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "orders", "{{order_id}}"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Lấy Payment theo Order thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().orderId).to.eql(pm.environment.get('order_id'));",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "4.6 Ghi nhận COD đã thu (cod-collected)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{customer_token}}"},
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/orders/{{order_id}}/cod-collected",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "orders", "{{order_id}}", "cod-collected"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Ghi nhận COD chuyển sang PAID (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body.status).to.eql('PAID');",
                                    "    pm.expect(body.paidAt).to.not.be.null;",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "4.7 [Negative] Tạo online attempt cho COD (409)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/{{payment_id}}/attempts",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "{{payment_id}}", "attempts"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('COD không thể tạo online attempt (409 PAYMENT_METHOD_NOT_ONLINE)', function () {",
                                    "    pm.response.to.have.status(409);",
                                    "    pm.expect(pm.response.json().code).to.eql('PAYMENT_METHOD_NOT_ONLINE');",
                                    "});"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "05. Các phương thức Online (Mục 7)",
            "item": [
                {
                    "name": "5.1 Tạo VNPay trả trước (PREPAID)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"orderId\": \"{{order_id}}\",\n  \"customerId\": \"{{customer_id}}\",\n  \"amountVnd\": 150000,\n  \"timing\": \"PREPAID\",\n  \"method\": \"VNPAY\",\n  \"correlationId\": \"{{correlation_id}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/order-context",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "order-context"]
                        }
                    },
                    "event": [
                        {
                            "listen": "prerequest",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.environment.set('order_id', pm.variables.replaceIn('{{$guid}}'));",
                                    "pm.environment.set('correlation_id', pm.variables.replaceIn('{{$guid}}'));"
                                ]
                            }
                        },
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Tạo VNPay thành công có redirectUrl sandbox (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body.redirectUrl).to.include('vnpayment.vn');",
                                    "    pm.environment.set('payment_id', body.id);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "5.2 Tạo ZaloPay trả trước (PREPAID)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"orderId\": \"{{$guid}}\",\n  \"customerId\": \"{{customer_id}}\",\n  \"amountVnd\": 150000,\n  \"timing\": \"PREPAID\",\n  \"method\": \"ZALOPAY\",\n  \"correlationId\": \"{{$guid}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/order-context",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "order-context"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Tạo ZaloPay thành công có order_url (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body.redirectUrl).to.include('zalopay.vn');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "5.3 Tạo PayOS trả trước (PREPAID)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"orderId\": \"{{$guid}}\",\n  \"customerId\": \"{{customer_id}}\",\n  \"amountVnd\": 150000,\n  \"timing\": \"PREPAID\",\n  \"method\": \"PAYOS\",\n  \"correlationId\": \"{{$guid}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/order-context",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "order-context"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Tạo PayOS thành công có checkoutUrl (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body.redirectUrl).to.include('payos.vn');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "5.4 Tạo VietQR Ngân hàng (PREPAID)",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"orderId\": \"{{$guid}}\",\n  \"customerId\": \"{{customer_id}}\",\n  \"amountVnd\": 150000,\n  \"timing\": \"PREPAID\",\n  \"method\": \"BANK_QR\",\n  \"correlationId\": \"{{$guid}}\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/order-context",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "order-context"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Tạo VietQR thành công có QR URL (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    pm.expect(body.redirectUrl).to.include('vietqr.app');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "5.5 Tạo hoặc lấy lại attempt online",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "{{internal_key}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/{{payment_id}}/attempts",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "{{payment_id}}", "attempts"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Lấy lại attempt online còn hiệu lực (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "5.6 Lấy reference từ lịch sử attempt (Admin)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{admin_token}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/{{payment_id}}/attempts",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "{{payment_id}}", "attempts"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Lấy danh sách attempts thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const body = pm.response.json();",
                                    "    const item = Array.isArray(body) ? body[0] : (body.content ? body.content[0] : null);",
                                    "    pm.expect(item).to.not.be.null;",
                                    "    pm.environment.set('payment_reference', item.reference);",
                                    "    console.log('payment_reference set to: ' + item.reference);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "5.7 Customer đọc trạng thái theo reference",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{customer_token}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/return-status?reference={{payment_reference}}",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "return-status"],
                            "query": [
                                {"key": "reference", "value": "{{payment_reference}}"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Customer đọc return-status thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "5.8 VNPay alias return-status",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{customer_token}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/vnpay/return-status?vnp_TxnRef={{payment_reference}}",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "vnpay", "return-status"],
                            "query": [
                                {"key": "vnp_TxnRef", "value": "{{payment_reference}}"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('VNPay alias return-status thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "06. Callback & Webhook (Mục 8)",
            "item": [
                {
                    "name": "6.1 VNPay IPN - Sai chữ ký (RspCode 97)",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/vnpay/ipn?vnp_TxnRef={{payment_reference}}&vnp_Amount=15000000&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_TransactionNo=TEST-001&vnp_SecureHash=invalid",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "vnpay", "ipn"],
                            "query": [
                                {"key": "vnp_TxnRef", "value": "{{payment_reference}}"},
                                {"key": "vnp_Amount", "value": "15000000"},
                                {"key": "vnp_ResponseCode", "value": "00"},
                                {"key": "vnp_TransactionStatus", "value": "00"},
                                {"key": "vnp_TransactionNo", "value": "TEST-001"},
                                {"key": "vnp_SecureHash", "value": "invalid"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('VNPay IPN phát hiện sai hash trả RspCode 97', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().RspCode).to.eql('97');",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "6.2 ZaloPay callback - Sai chữ ký MAC",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"data\": \"{\\\"app_trans_id\\\":\\\"{{payment_reference}}\\\",\\\"amount\\\":150000,\\\"zp_trans_id\\\":\\\"TEST-ZP-001\\\"}\",\n  \"mac\": \"invalid\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/zalopay/callback",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "zalopay", "callback"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('ZaloPay phát hiện sai MAC trả return_code -1', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    pm.expect(pm.response.json().return_code).to.eql(-1);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "6.3 PayOS webhook - Sai checksum",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"success\": true,\n  \"data\": {\n    \"orderCode\": 123456,\n    \"amount\": 150000,\n    \"reference\": \"TEST-PAYOS-001\",\n    \"code\": \"00\"\n  },\n  \"signature\": \"invalid\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/payos/webhook",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "payos", "webhook"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('PayOS phát hiện sai checksum trả success=false', function () {",
                                    "    pm.expect(pm.response.json().success).to.eql(false);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "6.4 SePay webhook - Sai chữ ký",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "X-SePay-Timestamp", "value": "1700000000"},
                            {"key": "X-SePay-Signature", "value": "invalid"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"id\": \"TEST-SEPAY-001\",\n  \"code\": \"{{payment_reference}}\",\n  \"transferAmount\": 150000,\n  \"transferType\": \"in\"\n}"
                        },
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/sepay/webhook",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "sepay", "webhook"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('SePay phát hiện sai signature trả success=false', function () {",
                                    "    pm.expect(pm.response.json().success).to.eql(false);",
                                    "});"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "07. Quản trị & Audit & Bảo mật (Mục 9 & 11)",
            "item": [
                {
                    "name": "7.1 Danh sách Payments (Admin)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{admin_token}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments?page=0&size=20",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments"],
                            "query": [
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "20"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Admin xem danh sách payment thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "7.2 Lịch sử attempt (Admin)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{admin_token}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/{{payment_id}}/attempts",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "{{payment_id}}", "attempts"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Admin xem lịch sử attempt thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "7.3 Audit callback (Admin)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{admin_token}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments/{{payment_id}}/callback-audits",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments", "{{payment_id}}", "callback-audits"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Admin xem callback audits thành công (200)', function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "7.4 [Negative] Customer gọi API admin (403 Forbidden)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{customer_token}}"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments?page=0&size=20",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments"],
                            "query": [
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "20"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Customer gọi admin bị chặn với 403 Forbidden', function () {",
                                    "    pm.response.to.have.status(403);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "7.5 [Negative] Không truyền Token gọi API admin (401 Unauthorized)",
                    "request": {
                        "method": "GET",
                        "header": [],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/payments?page=0&size=20",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "payments"],
                            "query": [
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "20"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Không truyền token bị chặn với 401 Unauthorized', function () {",
                                    "    pm.response.to.have.status(401);",
                                    "});"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "7.6 [Negative] Sai Internal Key (401 hoặc 403)",
                    "request": {
                        "method": "GET",
                        "header": [
                            {"key": "X-Internal-Api-Key", "value": "wrong-internal-key-12345"}
                        ],
                        "url": {
                            "raw": "{{gateway_url}}/api/v1/locations/validate?provinceId={{province_id}}&wardId={{ward_id}}",
                            "host": ["{{gateway_url}}"],
                            "path": ["api", "v1", "locations", "validate"],
                            "query": [
                                {"key": "provinceId", "value": "{{province_id}}"},
                                {"key": "wardId", "value": "{{ward_id}}"}
                            ]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test('Sai Internal API Key bị từ chối 401 hoặc 403', function () {",
                                    "    pm.expect([401, 403]).to.include(pm.response.code);",
                                    "});"
                                ]
                            }
                        }
                    ]
                }
            ]
        }
    ]
}

with open("e:/dynamicmart/postman/DynamicMart_Nguoi_4.postman_collection.json", "w", encoding="utf-8") as f:
    json.dump(collection, f, indent=2, ensure_ascii=False)

print("Created DynamicMart_Nguoi_4.postman_collection.json successfully")
