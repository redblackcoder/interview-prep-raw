package com.practice.interview;

import java.util.List;
import org.junit.Test;

public class ParserTest {
    private static final String TEST_JSON = """
                                  [
                        {
                          "request": {
                            "url": "/v1/charges",
                            "headers": {
                              "X-Client-User-Agent": "{\"lang\": \"ruby\", \"publisher\": \"example\", \"uname\": \"Linux version 3.13.0-57-generic (buildd@brownie) (gcc version 4.8.2 (Ubuntu 4.8.2-19ubuntu1) ) #95-Ubuntu SMP Fri Jun 19 09:28:15 UTC 2015\", \"hostname\": \"caron\", \"lang_version\": \"2.1.2 p95 (2014-05-08)\", \"engine\": \"ruby\", \"platform\": \"x86_64-linux\", \"bindings_version\": \"1.23.0\"}",
                              "Host": "api.example.com",
                              "Accept-Encoding": "gzip, deflate",
                              "Authorization": "Bearer sk_test_MASKED",
                              "Content-Type": "application/x-www-form-urlencoded",
                              "Accept": "*/*; q=0.5, application/xml",
                              "User-Agent": "ApiClient/v1 RubyBindings/1.23.0"
                            },
                            "body": "amount=123&currency=usd&card[number]=4242424242424242&card[exp_month]=12&card[exp_year]=2070",
                            "method": "POST"
                          },
                          "response": {
                            "body": "{\n  \"id\": \"ch_16T3toKv5xscqGyh90sf8TSa\",\n  \"amount\": 123,\n  \"amount_refunded\": 0,\n  \"application_fee\": null,\n  \"balance_transaction\": \"txn_16T3toKv5xscqGyhqAKF6jOq\",\n  \"captured\": true,\n  \"created\": 1437973900,\n  \"currency\": \"usd\",\n  \"customer\": null,\n  \"description\": null,\n  \"destination\": null,\n  \"dispute\": null,\n  \"failure_code\": null,\n  \"failure_message\": null,\n  \"fraud_details\": {\n  },\n  \"invoice\": null,\n  \"livemode\": false,\n  \"metadata\": {\n  },\n  \"object\": \"charge\",\n  \"paid\": true,\n  \"receipt_email\": null,\n  \"receipt_number\": null,\n  \"refunded\": false,\n  \"refunds\": {\n    \"has_more\": false,\n    \"total_count\": 0,\n    \"object\": \"list\",\n    \"data\": [\n\n    ],\n    \"url\": \"/v1/charges/ch_16T3toKv5xscqGyh90sf8TSa/refunds\"\n  },\n  \"shipping\": null,\n  \"source\": {\n    \"address_state\": null,\n    \"last4\": \"4242\",\n    \"dynamic_last4\": null,\n    \"address_zip_check\": null,\n    \"address_country\": null,\n    \"id\": \"card_16T3toKv5xscqGyh4LZ6rPyz\",\n    \"address_line2\": null,\n    \"address_line1\": null,\n    \"funding\": \"credit\",\n    \"metadata\": {\n    },\n    \"cvc_check\": null,\n    \"exp_month\": 12,\n    \"tokenization_method\": null,\n    \"address_line1_check\": null,\n    \"brand\": \"Visa\",\n    \"object\": \"card\",\n    \"fingerprint\": \"70RrlzfthHpu3e4l\",\n    \"exp_year\": 2070,\n    \"address_zip\": null,\n    \"customer\": null,\n    \"address_city\": null,\n    \"name\": null,\n    \"country\": \"US\"\n  },\n  \"statement_descriptor\": null,\n  \"status\": \"succeeded\"\n}",
                            "headers": {
                              "Content-Length": "1422",
                              "Request-Id": "req_6gQly7HiMYbIEC",
                              "Strict-Transport-Security": "max-age=31556926; includeSubDomains",
                              "Api-Version": "2015-07-13",
                              "Server": "nginx",
                              "Connection": "keep-alive",
                              "Cache-Control": "no-cache, no-store",
                              "Date": "Mon, 27 Jul 2015 05:11:40 GMT",
                              "Access-Control-Allow-Credentials": "true",
                              "Access-Control-Allow-Methods": "GET, POST, HEAD, OPTIONS, DELETE",
                              "Content-Type": "application/json",
                              "Access-Control-Allow-Origin": "*"
                            },
                            "code": 200
                          }
                        },
                        {
                          "request": {
                            "url": "/v1/charges",
                            "headers": {
                              "X-Client-User-Agent": "{\"lang\": \"ruby\", \"publisher\": \"example\", \"uname\": \"Linux version 3.13.0-57-generic (buildd@brownie) (gcc version 4.8.2 (Ubuntu 4.8.2-19ubuntu1) ) #95-Ubuntu SMP Fri Jun 19 09:28:15 UTC 2015\", \"hostname\": \"caron\", \"lang_version\": \"2.1.2 p95 (2014-05-08)\", \"engine\": \"ruby\", \"platform\": \"x86_64-linux\", \"bindings_version\": \"1.23.0\"}",
                              "Host": "api.example.com",
                              "Accept-Encoding": "gzip, deflate",
                              "Authorization": "Bearer sk_test_MASKED",
                              "Content-Type": "application/x-www-form-urlencoded",
                              "Accept": "*/*; q=0.5, application/xml",
                              "User-Agent": "ApiClient/v1 RubyBindings/1.23.0"
                            },
                            "body": "amount=123&currency=usd&card[number]=4242424242424242&card[exp_month]=12&card[exp_year]=2070&card[address_line1]=3180+18th+St&card[address_zip]=94110",
                            "method": "POST"
                          },
                          "response": {
                            "body": "{\n  \"id\": \"ch_16T4RoKv5xscqGyhKvytKEuG\",\n  \"amount\": 123,\n  \"amount_refunded\": 0,\n  \"application_fee\": null,\n  \"balance_transaction\": \"txn_16T4RoKv5xscqGyhUtgdxIrD\",\n  \"captured\": true,\n  \"created\": 1437976008,\n  \"currency\": \"usd\",\n  \"customer\": null,\n  \"description\": null,\n  \"destination\": null,\n  \"dispute\": null,\n  \"failure_code\": null,\n  \"failure_message\": null,\n  \"fraud_details\": {\n  },\n  \"invoice\": null,\n  \"livemode\": false,\n  \"metadata\": {\n  },\n  \"object\": \"charge\",\n  \"paid\": true,\n  \"receipt_email\": null,\n  \"receipt_number\": null,\n  \"refunded\": false,\n  \"refunds\": {\n    \"has_more\": false,\n    \"total_count\": 0,\n    \"object\": \"list\",\n    \"data\": [\n\n    ],\n    \"url\": \"/v1/charges/ch_16T4RoKv5xscqGyhKvytKEuG/refunds\"\n  },\n  \"shipping\": null,\n  \"source\": {\n    \"address_state\": null,\n    \"last4\": \"4242\",\n    \"dynamic_last4\": null,\n    \"address_zip_check\": \"pass\",\n    \"address_country\": null,\n    \"id\": \"card_16T4RoKv5xscqGyhav15111M\",\n    \"address_line2\": null,\n    \"address_line1\": \"3180 18th St\",\n    \"funding\": \"credit\",\n    \"metadata\": {\n    },\n    \"cvc_check\": null,\n    \"exp_month\": 12,\n    \"tokenization_method\": null,\n    \"address_line1_check\": \"pass\",\n    \"brand\": \"Visa\",\n    \"object\": \"card\",\n    \"fingerprint\": \"70RrlzfthHpu3e4l\",\n    \"exp_year\": 2070,\n    \"address_zip\": \"94110\",\n    \"customer\": null,\n    \"address_city\": null,\n    \"name\": null,\n    \"country\": \"US\"\n  },\n  \"statement_descriptor\": null,\n  \"status\": \"succeeded\"\n}",
                            "headers": {
                              "Content-Length": "1439",
                              "Access-Control-Max-Age": "300",
                              "Request-Id": "req_6gRKkuyOC7S0QT",
                              "Strict-Transport-Security": "max-age=31556926; includeSubDomains",
                              "Api-Version": "2015-07-13",
                              "Server": "nginx",
                              "Connection": "keep-alive",
                              "Cache-Control": "no-cache, no-store",
                              "Date": "Mon, 27 Jul 2015 05:46:48 GMT",
                              "Access-Control-Allow-Credentials": "true",
                              "Access-Control-Allow-Methods": "GET, POST, HEAD, OPTIONS, DELETE",
                              "Content-Type": "application/json",
                              "Access-Control-Allow-Origin": "*"
                            },
                            "code": 200
                          }
                        }
            ]
                                  """;

    @Test
    public void validJson() {
        Parser parser = new Parser();
        List<MyLog> logList = parser.parseContent(TEST_JSON);
        System.out.println(logList.size());
    }

}
