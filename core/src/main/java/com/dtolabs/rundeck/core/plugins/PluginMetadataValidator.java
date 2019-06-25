/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.VersionConstants;

import java.util.Arrays;
import java.util.List;

public class PluginMetadataValidator {

    private static List<String> HOST_TYPES = Arrays.asList("all","unix","windows");
    private static String INCOMPATIBLE_PLUGIN_VER_MSG = "Plugin is not compatible with this version of Rundeck";
    public final static String OS_TYPE = System.getProperty("os.name").toLowerCase();

    public static PluginValidation.State validateTargetHostCompatibility(
            final List<String> errors,
            final String targetHostCompatibility
    ) {
        if(targetHostCompatibility == null) {
            errors.add("No targetHostCompatibility property specified in metadata");
            return PluginValidation.State.INVALID;
        }
        if(targetHostCompatibility.equals("all")) return PluginValidation.State.VALID;
        if(!HOST_TYPES.contains(targetHostCompatibility)) {
            errors.add("Unknown target host type specified: " + targetHostCompatibility + ". Allowed types: " +
                       Arrays.toString(HOST_TYPES.toArray()));
            return PluginValidation.State.INVALID;
        }

        if((targetHostCompatibility.equals("unix") && OS_TYPE.startsWith("windows")) ||
           (targetHostCompatibility.equals("windows") && !OS_TYPE.startsWith("windows"))) {
            errors.add("Plugin target host("+targetHostCompatibility+") is incompatible with this Rundeck instance: " + OS_TYPE);
            return PluginValidation.State.INCOMPATIBLE;
        }
        return PluginValidation.State.VALID;
    }

    public static PluginValidation.State validateRundeckCompatibility(
            final List<String> errors,
            final String rundeckCompatibilityVersion
    ) {
        if(rundeckCompatibilityVersion == null) {
            errors.add("rundeckCompatibilityVersion cannot be null in metadata");
            return PluginValidation.State.INVALID;
        }
        VersionCompare rundeckVer = VersionCompare.forString(VersionConstants.VERSION);
        VersionCompare compatVer = VersionCompare.forString(rundeckCompatibilityVersion);
        if(!compatVer.majString.equals(rundeckVer.majString)) {
            errors.add(INCOMPATIBLE_PLUGIN_VER_MSG);
            return PluginValidation.State.INCOMPATIBLE;
        }
        if(compatVer.minString.equals("x")) return PluginValidation.State.VALID;
        Integer cmin = new Integer(compatVer.minString.replaceAll("\\+",""));
        if(rundeckVer.min > cmin) return PluginValidation.State.VALID;
        if(!checkVer(rundeckVer.min,compatVer.minString)) {
            errors.add(INCOMPATIBLE_PLUGIN_VER_MSG);
            return PluginValidation.State.INCOMPATIBLE;
        }
        if(compatVer.patchString == null) return PluginValidation.State.VALID;
        if(!checkVer(rundeckVer.patch,compatVer.patchString)) {
            errors.add(INCOMPATIBLE_PLUGIN_VER_MSG);
            return PluginValidation.State.INCOMPATIBLE;
        }
        return PluginValidation.State.VALID;
    }

    private static boolean checkVer(final Integer rdVer, final String compVer) {
        if(compVer == null) return false;
        if(compVer.contains("x")) return true;
        boolean greater = compVer.contains("+");
        Integer icver = new Integer(compVer.replaceAll("\\+",""));
        return compare(rdVer,icver,greater);
    }

    static boolean compare(Integer rd, Integer compat, boolean greater) {
        if(greater && rd >= compat) return true;
        return compat.equals(rd);
    }
}
