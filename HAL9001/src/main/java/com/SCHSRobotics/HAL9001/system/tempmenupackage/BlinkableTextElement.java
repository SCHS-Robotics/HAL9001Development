package com.SCHSRobotics.HAL9001.system.tempmenupackage;

import com.SCHSRobotics.HAL9001.util.exceptions.ExceptionChecker;

import java.util.ArrayList;
import java.util.List;

//Todo, may not keep
public class BlinkableTextElement extends TextElement implements ViewListener, Blinkable, MutableTextViewElement {
    private char[] blinkingCharArray;
    private String unmodifiedText, text;
    private HALMenu.BlinkState currentBlinkState;
    private List<Integer> blinkingIndices;
    private List<Character> blinkingCharacters;
    private boolean blinkingEnabled;

    public BlinkableTextElement(String text) {
        super(text);
        blinkingCharArray = text.toCharArray();
        unmodifiedText = text;
        this.text = unmodifiedText;
        currentBlinkState = HALMenu.BlinkState.OFF;
        blinkingIndices = new ArrayList<>();
        blinkingCharacters = new ArrayList<>();
        blinkingEnabled = true;
    }


    @Override
    public boolean update() {
        if(!blinkingEnabled) {
            text = unmodifiedText;
            return false;
        }
        if(currentBlinkState == HALMenu.BlinkState.ON) {
            text = new String(blinkingCharArray);
        }
        else {
            text = unmodifiedText;
        }
        return false;
    }

    @Override
    public void notifyCurrentBlinkState(HALMenu.BlinkState blinkState) {
        currentBlinkState = blinkState;
    }

    @Override
    public String getText() {
        if(!blinkingEnabled) {
            text = unmodifiedText;
        }
        if(currentBlinkState == HALMenu.BlinkState.ON) {
            text = new String(blinkingCharArray);
        }
        else {
            text = unmodifiedText;
        }
        return text;
    }

    public BlinkableTextElement blinkCharAt(int charIdx, char charToBlink) {
        ExceptionChecker.assertTrue(charIdx < unmodifiedText.length(), new IndexOutOfBoundsException("Blink Index out of bounds."));
        ExceptionChecker.assertFalse(charIdx < 0, new IndexOutOfBoundsException("Blink index must be greater than or equal to 0."));
        blinkingCharArray[charIdx] = charToBlink;
        blinkingIndices.add(charIdx);
        blinkingCharacters.add(charToBlink);
        return this;
    }

    public void removeBlinkingChar(int charIdx) {
        ExceptionChecker.assertTrue(charIdx < unmodifiedText.length(), new IndexOutOfBoundsException("Char Index out of bounds."));
        ExceptionChecker.assertFalse(charIdx < 0, new IndexOutOfBoundsException("Char index must be greater than or equal to 0."));
        char[] unmodifiedCharArray = unmodifiedText.toCharArray();
        blinkingCharArray[charIdx] = unmodifiedCharArray[charIdx];
    }

    public void removeAllBlinkingChars() {
        blinkingCharArray = unmodifiedText.toCharArray();
        blinkingIndices.clear();
        blinkingCharacters.clear();
    }

    public String getUnmodifiedText() {
        return unmodifiedText;
    }

    @Override
    public void setText(String text) {
        unmodifiedText = text;
        blinkingCharArray = unmodifiedText.toCharArray();
        List<Integer> newBlinkingIndices = new ArrayList<>();
        List<Character> newBlinkingCharacters = new ArrayList<>();
        for (int i = 0; i < blinkingIndices.size(); i++) {
            int idx = blinkingIndices.get(i);
            if(idx < unmodifiedText.length()) {
                newBlinkingIndices.add(idx);
                newBlinkingCharacters.add(blinkingCharacters.get(i));
                blinkingCharArray[idx] = blinkingCharacters.get(i);
            }
        }
        blinkingIndices.clear();
        blinkingCharacters.clear();
        blinkingIndices.addAll(newBlinkingIndices);
        blinkingCharacters.addAll(newBlinkingCharacters);
    }

    @Override
    public void append(char c) {
        unmodifiedText += c;
        char[] newBlinkingCharArray = new char[unmodifiedText.length()];
        System.arraycopy(blinkingCharArray, 0, newBlinkingCharArray, 0, blinkingCharArray.length);
        newBlinkingCharArray[unmodifiedText.length() - 1] = c;
        blinkingCharArray = newBlinkingCharArray;
    }

    @Override
    public void remove(int charIdx) {
        ExceptionChecker.assertTrue(charIdx < unmodifiedText.length(), new IndexOutOfBoundsException("Char Index out of bounds."));
        StringBuilder unmodifiedBuilder = new StringBuilder().append(unmodifiedText);
        unmodifiedBuilder.deleteCharAt(charIdx);
        unmodifiedText = unmodifiedBuilder.toString();
        char[] newBlinkingCharArray = new char[unmodifiedText.length()];
        List<Integer> newBlinkingIndices = new ArrayList<>();
        List<Character> newBlinkingCharacters = new ArrayList<>();
        for (int i = 0; i < blinkingIndices.size(); i++) {
            int idx = blinkingIndices.get(i);
            if(idx >= charIdx && idx != unmodifiedText.length()) {
                newBlinkingIndices.add(idx - 1);
                newBlinkingCharArray[idx - 1] = blinkingCharacters.get(i);
                newBlinkingCharacters.add(blinkingCharacters.get(i));
            }
            else if(idx != unmodifiedText.length()) {
                newBlinkingIndices.add(idx);
                newBlinkingCharArray[idx] = blinkingCharacters.get(i);
                newBlinkingCharacters.add(blinkingCharacters.get(i));
            }
        }
        blinkingCharArray = newBlinkingCharArray;
        blinkingIndices.clear();
        blinkingCharacters.clear();
        blinkingIndices.addAll(newBlinkingIndices);
        blinkingCharacters.addAll(newBlinkingCharacters);
    }

    @Override
    public void setBlinkEnabled(boolean blinkEnabled) {
        blinkingEnabled = blinkEnabled;
    }

    @Override
    public void setChar(int charIdx, char c) {
        ExceptionChecker.assertTrue(charIdx < unmodifiedText.length(), new IndexOutOfBoundsException("Char Index out of bounds."));
        if(!blinkingIndices.contains(charIdx)) {
            blinkingCharArray[charIdx] = c;
        }
        char[] unmodifiedCharArray = unmodifiedText.toCharArray();
        unmodifiedCharArray[charIdx] = c;
        unmodifiedText = new String(unmodifiedCharArray);
    }
}
