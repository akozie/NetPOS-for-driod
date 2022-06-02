package com.woleapp.netpos.contactless.taponphone.mastercard.implementations;

import com.mastercard.terminalsdk.listeners.UnpredictableNumberProvider;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

public class UnpredictableNumberImplementation implements UnpredictableNumberProvider {
    @Override
    public byte[] generateRandomBytes(int size) {
        // Letting the system choose the best random number generator, instead
        // of specifying a random generator;
        SecureRandom randomGenerator = new SecureRandom();
        byte[] randomNumber = new byte[size];
        randomGenerator.nextBytes(randomNumber);

        // Create a byte buffer of multiple of 8 bytes and store the random
        // number
        ByteBuffer checkBuffer = ByteBuffer.allocate(randomNumber.length
                + (8 - (randomNumber.length % 8)));
        checkBuffer.put(randomNumber);

        return randomNumber;
    }
}
