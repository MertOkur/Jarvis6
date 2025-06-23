package com.example.jarvis6;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class OfflineTextSummarizer {

    // Beispiel-Stopwort-Liste (für Deutsch)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "der", "die", "das", "und", "ist", "in", "ich", "du", "er", "sie", "es", "wir", "ihr", "sie",
            "ein", "eine", "einer", "einem", "einen", "den", "dem", "des", "zu", "von", "mit", "als",
            "für", "auf", "an", "im", "am", "zum", "zur", "oder", "aber", "auch", "nicht", "noch", "nur",
            "kann", "können", "muss", "müssen", "soll", "sollen", "will", "wollen", "hat", "haben", "sein",
            "wird", "werden", "hier", "dort", "dabei", "dazu", "darum", "danach", "dann", "doch", "mal",
            "um", "über", "unter", "vor", "nach", "aus", "bei", "gegen", "ohne", "seit", "trotz", "während",
            "wegen", "durch", "bis", "jedoch", "sowie", "daher", "somit", "indem", "obwohl", "sowohl", "alsauch"
    ));

    private SentenceDetectorME sentenceDetector;
    private TokenizerME tokenizer;

    public OfflineTextSummarizer(Context context) {
        try {
            var sentModelIn = context.getAssets().open("/de-sent.bin");
            var sentModel = new SentenceModel(sentModelIn);
            sentenceDetector = new SentenceDetectorME(sentModel);

            var tokenModelIn = context.getAssets().open("/de-token.bin");
            var tokenModel = new TokenizerModel(tokenModelIn);
            tokenizer = new TokenizerME(tokenModel);

            sentModelIn.close();
            tokenModelIn.close();
        } catch (IOException e) {
            System.err.println("Fehler beim Laden den OpenNLP Modelle: " + e.getMessage());
            sentenceDetector = null;
            tokenizer = null;
        }
    }

    public String summarize(String text, int numSentences) {
        var sentences = segmentSentences(text);
        if (sentences.isEmpty()) {
            return "Keine Sätze zum Zusammenfassen gefunden.";
        }

        Map<String, Integer> wordFrequencies = calculateWordFrequencies(sentences);

        List<SentenceScore> sentenceScores = new ArrayList<>();
        for(int i=0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = calculateSentenceScore(sentence, wordFrequencies);
            if (i == 0 || i == sentences.size() - 1)
                score += 0.5;

            sentenceScores.add(new SentenceScore(sentence, score, i));
        }

        sentenceScores.sort((s1, s2) -> Double.compare(s1.score(), s2.score()));

        List<SentenceScore> topSentences = sentenceScores.stream().limit(numSentences).collect(Collectors.toList());

        topSentences.sort(Comparator.comparingInt(s -> s.originalIndex()));

        var summary = new StringBuilder();
        for (var ss : topSentences) {
            summary.append(ss.sentence()).append(" ");
        }

        return summary.toString().trim();
    }

    private List<String> segmentSentences(String text) {
        if (sentenceDetector == null) {
            System.err.println("OpenNLP SentenceDetector nicht geladen: ");
            return Arrays.stream(text.split("(?<=[.?!])\\s+(?=[A-ZÄÖÜß])|(?<=[.?!])\\s*$"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return Arrays.asList(sentenceDetector.sentDetect(text));
    }

    private List<String> tokenizeAndClean(String sentence) {
        if (tokenizer == null) {
            System.err.println("OpenNLP Tokenizer nicht geladen: ");
            return Arrays.stream(sentence.toLowerCase().split("\\W+"))
                    .filter(word -> !word.isEmpty() && !STOP_WORDS.contains(word))
                    .collect(Collectors.toList());
        }

        return Arrays.stream(tokenizer.tokenize(sentence))
                .map(String::toLowerCase)
                .filter(word -> !word.isEmpty() && !STOP_WORDS.contains(word) && Character.isLetter(word.charAt(0)))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> calculateWordFrequencies(List<String> sentences) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (var sentence : sentences) {
            var words = tokenizeAndClean(sentence);
            for (var word : words) {
                frequencies.put(word, frequencies.getOrDefault(word, 0) + 1);
            }
        }

        return frequencies;
    }

    private double calculateSentenceScore(String sentence, Map<String, Integer> wordFrequencies) {
        double score = 0;
        List<String> words = tokenizeAndClean(sentence);

        for (var word : words) {
            score += wordFrequencies.getOrDefault(word, 0);
        }

        if (!words.isEmpty()) {
            score /= words.size();
        }

        return score;
    }
}
