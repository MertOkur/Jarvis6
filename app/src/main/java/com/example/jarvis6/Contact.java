package com.example.jarvis6;

import java.util.List;

public record Contact(String id, String name, List<String> phoneNumber, List<String> email, List<String> adress) {}
