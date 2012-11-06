/*
 * Copyright 2011 Ancestry.com and Foundation for On-Line Genealogy, Inc.
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
package org.folg.places.standardize;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;

/**
 * Normalize a place text
 */
public class Normalizer {
   private static Logger logger = Logger.getLogger("org.folg.places.search");
   private static Normalizer normalizer = new Normalizer();
   private final Map<Character, String> characterReplacements;

   public static Normalizer getInstance() {
      return normalizer;
   }

   private Normalizer() {
      // read properties file
      try {
         Properties props = new Properties();
         props.load(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("place-normalizer.properties"), "UTF8"));
         // build character replacements
         characterReplacements = new HashMap<Character, String>();
         for (String replacement : props.getProperty("characterReplacements").split(",")) {
            characterReplacements.put(replacement.charAt(0), replacement.substring(2));
         }
      } catch (IOException e) {
         throw new RuntimeException("place-normalizer.properties not found");
      }
   }

   /**
    * Tokenize name by removing diacritics, lowercasing, and splitting on non alphanumeric characters
    *
    * @param text string to tokenize
    * @return tokenized place levels
    */
   public List<List<String>> tokenize(String text) {
      List<List<String>> levels = new ArrayList<List<String>>();

      // find the last letter
      int lastPos = text.length()-1;
      while (lastPos >= 0) {
         char c = text.charAt(lastPos);

         // break on letter (assume any character > 127 is alphabetic
         if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c > 127)) {
            break;
         }
         lastPos--;
      }

      List<String> levelWords = new ArrayList<String>();
      StringBuilder buf = new StringBuilder();

      // parse up to and including the last letter; anything after that is junk
      for (int i = 0; i <= lastPos; i++) {
         char c = text.charAt(i);
         String replacement;

         if (c == ',') {
            if (buf.length() > 0) {
               levelWords.add(buf.toString());
               buf.setLength(0);
            }
            if (levelWords.size() > 0) {
               levels.add(levelWords);
               levelWords = new ArrayList<String>();
            }
         }
         else if ((replacement = characterReplacements.get(c)) != null) {
            buf.append(replacement.toLowerCase());
         }
         else if (c >= 'A' && c <= 'Z') {
            buf.append(Character.toLowerCase(c));
         }
         else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
            buf.append(c);
         }
         else if (Character.isLetter(c)) {
            // ignore letters > U+0250; they're generally from scripts that don't map well to roman letters
            // ignore 186,170: superscript o and a used in spanish numbers: 1^a and 2^o
            // ignore 440,439: Ezh and reverse-Ezh; the only times they appear in the data is in what appears to be noise
            if (c < 592 && c != 186 && c != 170 && c != 439 && c != 440) {
               logger.warning("Untokenized letter:" + c + " (" + (int) c + ") in " + text);
            }
         }
         // tokenize words on non-alphanumeric
         else {
            if (buf.length() > 0) {
               levelWords.add(buf.toString());
               buf.setLength(0);
            }
         }
      }
      if (buf.length() > 0) {
         levelWords.add(buf.toString());
      }

      if (levelWords.size() > 0) {
         levels.add(levelWords);
      }

      return levels;
   }

   /**
    * Remove diacritics, lowercase, and remove non alphanumeric characters
    *
    * @param text string to normalize
    * @return normalized name
    */
   public String normalize(String text) {
      return normalize(text, false);
   }

   /**
    * Remove diacritics, lowercase, and remove non alphanumeric characters
    *
    * @param text string to normalize
    * @param allowWildcards if true, keep ?* characters
    * @return normalized name
    */
   public String normalize(String text, boolean allowWildcards) {
      StringBuilder buf = new StringBuilder();

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         String replacement;

         if ((replacement = characterReplacements.get(c)) != null) {
            buf.append(replacement.toLowerCase());
         }
         else if (c >= 'A' && c <= 'Z') {
            buf.append(Character.toLowerCase(c));
         }
         else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
            buf.append(c);
         }
         else if (allowWildcards && (c == '?' || c == '*')) {
            buf.append(c);
         }
         else if (Character.isLetter(c)) {
            // ignore letters > U+0250; they're generally from scripts that don't map well to roman letters
            // ignore 186,170: superscript o and a used in spanish numbers: 1^a and 2^o
            // ignore 440,439: Ezh and reverse-Ezh; the only times they appear in the data is in what appears to be noise
            if (c < 592 && c != 186 && c != 170 && c != 439 && c != 440) {
               logger.warning("Untokenized letter:" + c + " (" + (int) c + ") in " + text);
            }
         }
      }

      return buf.toString();
   }
}
