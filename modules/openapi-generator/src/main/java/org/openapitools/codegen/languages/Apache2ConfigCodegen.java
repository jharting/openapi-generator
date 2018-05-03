/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.utils.ModelUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;


public class Apache2ConfigCodegen extends DefaultCodegen implements CodegenConfig {
    public static final String USER_INFO_PATH = "userInfoPath";
    protected String userInfoPath = "/var/www/html/";

    @Override
    public CodegenType getTag() {
        return CodegenType.CONFIG;
    }

    @Override
    public String getName() {
        return "apache2";
    }

    @Override
    public String getHelp() {
        return "Generates an Apache2 Config file with the permissions";
    }

    public Apache2ConfigCodegen() {
        super();
        apiTemplateFiles.put("apache-config.mustache", ".conf");

        embeddedTemplateDir = templateDir = "apache2";

        cliOptions.add(new CliOption(USER_INFO_PATH, "Path to the user and group files"));
    }


    @Override
    public void processOpts() {
        if (additionalProperties.containsKey(USER_INFO_PATH)) {
            userInfoPath = additionalProperties.get(USER_INFO_PATH).toString();
        }
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");
        List<CodegenOperation> newOpList = new ArrayList<CodegenOperation>();
        for (CodegenOperation op : operationList) {
            String path = new String(op.path);

            String[] items = path.split("/", -1);
            List<String> splitPath = new ArrayList<String>();
            for (String item : items) {
                if (item.matches("^\\{(.*)\\}$")) {
                    item = "*";
                }
                splitPath.add(item);
                op.path += item + "/";
            }
            op.vendorExtensions.put("x-codegen-userInfoPath", userInfoPath);
            boolean foundInNewList = false;
            for (CodegenOperation op1 : newOpList) {
                if (!foundInNewList) {
                    if (op1.path.equals(op.path)) {
                        foundInNewList = true;
                        List<CodegenOperation> currentOtherMethodList = (List<CodegenOperation>) op1.vendorExtensions.get("x-codegen-otherMethods");
                        if (currentOtherMethodList == null) {
                            currentOtherMethodList = new ArrayList<CodegenOperation>();
                        }
                        op.operationIdCamelCase = op1.operationIdCamelCase;
                        currentOtherMethodList.add(op);
                        op1.vendorExtensions.put("x-codegen-otherMethods", currentOtherMethodList);
                    }
                }
            }
            if (!foundInNewList) {
                newOpList.add(op);
            }
        }
        operations.put("operation", newOpList);
        return objs;
    }
}