/*
 * Copyright 2013 JavaANPR contributors
 * Copyright 2006 Ondrej Martinsky
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package net.sf.javaanpr.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public final class TestStatistics {

    private static final transient Logger logger = LoggerFactory.getLogger(TestStatistics.class);
    private static String helpText =
            "-----------------------------------------------------------\n" + "ANPR Statistics Generator\n"
                    + "Copyright (c) Ondrej Martinsky, 2006-2007\n" + "\n"
                    + "Licensed under the Educational Community License,\n" + "\n" + "Command line arguments\n" + "\n"
                    + "    -help         Displays this help\n" + "    -i <file>     Create statistics for test file\n"
                    + "\n" + "Test file must be have a CSV format\n"
                    + "Each row must contain name of analysed snapshot,\n" + "real plate and recognized plate string\n"
                    + "Example : \n" + "001.jpg, 1B01234, 1B012??";

    private TestStatistics() {
        // intentionally empty
    }

    public static void main(String[] args) throws IOException {
        if ((args.length == 2) && args[0].equals("-i")) { // proceed analysis
            File f = new File(args[1]);
            BufferedReader input = new BufferedReader(new FileReader(f));
            String line;
            int lineCount = 0;
            String[] split;
            TestReport testReport = new TestReport();
            while ((line = input.readLine()) != null) {
                lineCount++;
                split = line.split(",", 4);
                if (split.length != 3) {
                    logger.warn("Line ( {} ) contains invalid CSV data (skipping)", lineCount);
                    continue;
                }
                testReport.addRecord(testReport.new TestRecord(split[0], split[1], split[2]));
            }
            input.close();
            testReport.printStatistics();
        } else {
            System.out.println(TestStatistics.helpText);
        }

    }


    private static final class TestReport {

        private Vector<TestRecord> records;

        private TestReport() {
            this.records = new Vector<TestRecord>();
        }

        private void addRecord(TestRecord testRecord) {
            this.records.add(testRecord);
        }

        private void printStatistics() {
            int weightedScoreCount = 0;
            int binaryScoreCount = 0;
            int characterCount = 0;
            System.out.println("----------------------------------------------");
            System.out.println("Defective plates\n");

            for (TestRecord record : this.records) {
                characterCount += record.getLength();
                weightedScoreCount += record.getGoodCount();
                binaryScoreCount += (record.isOk() ? 1 : 0);
                if (!record.isOk()) {
                    System.out.println(record.plate + " ~ " + record.recognizedPlate + " (" + (
                            ((float) record.getGoodCount() / record.getLength()) * 100) + "% ok)");
                }
            }
            System.out.println("\n----------------------------------------------");
            System.out.println("Test report statistics\n");
            System.out.println("Total number of plates     : " + this.records.size());
            System.out.println("Total number of characters : " + characterCount);
            System.out.println(
                    "Binary score               : " + (((float) binaryScoreCount / this.records.size()) * 100));
            System.out.println("Weighted score             : " + (((float) weightedScoreCount / characterCount) * 100));
        }

        private final class TestRecord {
            private String name, plate, recognizedPlate;
            private int good;
            private int length;

            private TestRecord(String name, String plate, String recognizedPlate) {
                this.name = name.trim();
                this.plate = plate.trim();
                this.recognizedPlate = recognizedPlate.trim();
                this.compute();
            }

            private void compute() {
                this.length = Math.max(this.plate.length(), this.recognizedPlate.length());
                int g1 = 0;
                int g2 = 0;
                for (int i = 0; i < this.length; i++) { // Compare from the beginning (f.e. BA123AB vs. BA123ABX)
                    if (this.getChar(this.plate, i) == this.getChar(this.recognizedPlate, i)) {
                        g1++;
                    }
                }
                for (int i = 0; i < this.length; i++) { // Compare from the back (f.e. BA123AB vs. XBA123AB)
                    if (this.getChar(this.plate, this.length - i - 1) == this
                            .getChar(this.recognizedPlate, this.length - i - 1)) {
                        g2++;
                    }
                }
                this.good = Math.max(g1, g2);
            }

            private char getChar(String string, int position) {
                if (position >= string.length()) {
                    return ' ';
                }
                if (position < 0) {
                    return ' ';
                }
                return string.charAt(position);
            }

            public int getGoodCount() {
                return this.good;
            }

            public int getLength() {
                return this.length;
            }

            public boolean isOk() {
                return this.length == this.good;
            }
        }
    }
}
