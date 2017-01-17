/**
 *  Copyright 2016-2017 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.jboss.fuse.credential.store.karaf.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.jboss.fuse.credential.store.karaf.util.ProtectionType;

@Service
public class CredentialStoreProtectionCompletionSupport implements Completer {

    private static String optionOf(final String value) {
        if (value == null) {
            return "";
        }

        final int optionValueSeparatorIdx = value.indexOf('=');

        if ((optionValueSeparatorIdx > 0) && (optionValueSeparatorIdx < value.length())) {
            return value.substring(0, optionValueSeparatorIdx);
        }

        return value;
    }

    static Set<String> usedOptions(final String... arguments) {
        final Set<String> ret = new HashSet<>();

        for (int i = 0; i < arguments.length; i++) {
            final String argument = arguments[i];

            final boolean isProtectionAttributeOption = "-k".equals(argument)
                || "--protection-attributes".equals(argument);
            final boolean hasMoreArguments = arguments.length > (i + 1);
            if (isProtectionAttributeOption && hasMoreArguments && !arguments[i + 1].startsWith("-")) {
                ret.add(optionOf(arguments[i + 1]));
            }
        }

        return ret;
    }

    @Override
    public int complete(final Session session, final CommandLine commandLine, final List<String> candidates) {
        final String[] arguments = commandLine.getArguments();

        int protectionTypeIdx = -1;
        for (int i = 0; i < arguments.length; i++) {
            final String argument = arguments[i];
            if ("-p".equals(argument) || "--protection-type".equals(argument)) {
                protectionTypeIdx = i;
                break;
            }
        }

        if ((protectionTypeIdx < 0) || (arguments.length <= (protectionTypeIdx + 1))) {
            return -1;
        }

        final String protectionTypeString = arguments[protectionTypeIdx + 1];
        final ProtectionType protectionType;
        try {
            protectionType = ProtectionType.valueOf(protectionTypeString);
        } catch (final IllegalArgumentException e) {
            return -1;
        }

        final String[] supportedOptions = Arrays.stream(protectionType.getSupportedOptions()).sorted()
                .toArray(String[]::new);

        final String option = optionOf(commandLine.getCursorArgument());

        final Set<String> usedOptions = usedOptions(arguments);

        if (Arrays.binarySearch(supportedOptions, option) >= 0) {
            final String[] options = Arrays.stream(protectionType.getOptionValuesFor(option)).map(o -> option + "=" + o)
                    .toArray(String[]::new);

            return new StringsCompleter(options).complete(session, commandLine, candidates);
        }

        final int complete = new StringsCompleter(Arrays.stream(supportedOptions).filter(o -> !usedOptions.contains(o))
                .map(o -> o + "=").toArray(String[]::new)).complete(session, commandLine, candidates);

        if ((complete > 0) && (candidates.size() == 1)) {
            final String singleOption = candidates.get(0);

            candidates.set(0, singleOption.substring(0, singleOption.length() - 1));
        }

        return complete;
    }

}
