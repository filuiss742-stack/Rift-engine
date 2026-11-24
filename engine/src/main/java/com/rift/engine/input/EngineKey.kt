package com.rift.engine.input

/**
 * Platform-independent identifiers for common keyboard keys supported by
 * the Android and desktop adapters. Bind them to game-owned named actions
 * with [InputActionMap]. Platform-specific keys can still be forwarded through
 * GameEngine.onKey() using game-owned integer identifiers.
 */
object EngineKey {
    const val A = 1
    const val D = 2
    const val W = 3
    const val S = 4

    const val LEFT = 5
    const val RIGHT = 6
    const val UP = 7
    const val DOWN = 8

    const val SPACE = 9
    const val ENTER = 10
    const val ESCAPE = 11
    const val TAB = 12
    const val E = 13
    const val Q = 14
    const val F = 15
    const val R = 16
    const val SHIFT = 17
    const val CTRL = 18
    const val ALT = 19

    // Remaining letters, preserving the original public IDs above.
    const val B = 20
    const val C = 21
    const val G = 22
    const val H = 23
    const val I = 24
    const val J = 25
    const val K = 26
    const val L = 27
    const val M = 28
    const val N = 29
    const val O = 30
    const val P = 31
    const val T = 32
    const val U = 33
    const val V = 34
    const val X = 35
    const val Y = 36
    const val Z = 37

    const val DIGIT_0 = 38
    const val DIGIT_1 = 39
    const val DIGIT_2 = 40
    const val DIGIT_3 = 41
    const val DIGIT_4 = 42
    const val DIGIT_5 = 43
    const val DIGIT_6 = 44
    const val DIGIT_7 = 45
    const val DIGIT_8 = 46
    const val DIGIT_9 = 47

    const val F1 = 48
    const val F2 = 49
    const val F3 = 50
    const val F4 = 51
    const val F5 = 52
    const val F6 = 53
    const val F7 = 54
    const val F8 = 55
    const val F9 = 56
    const val F10 = 57
    const val F11 = 58
    const val F12 = 59

    const val BACKSPACE = 60
    const val DELETE = 61
    const val HOME = 62
    const val END = 63
    const val PAGE_UP = 64
    const val PAGE_DOWN = 65
    const val INSERT = 66
    const val CAPS_LOCK = 67
}
