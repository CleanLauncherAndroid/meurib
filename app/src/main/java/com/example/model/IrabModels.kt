package com.example.model

enum class IrabCategory {
    MARFOU,
    MANSOUB,
    MAJROUR,
    VERB_MABNI,
    PARTICLE,
    OTHER
}

data class AffixInfo(
    val form: String,
    val description: String?
)

data class MorphReading(
    val diacritized: String,
    val lemma: String,
    val stem: String,
    val root: String?,
    val pattern: String?,
    val partOfSpeech: String,
    val subType: String?,
    val tense: String?,
    val gender: String?,
    val number: String?,
    val person: String?,
    val pronoun: String?,
    val voice: String?,
    val state: String?,
    val caseOrMood: String?,
    val proclitics: List<AffixInfo>,
    val enclitics: List<AffixInfo>,
    val score: Double
)

data class RawTokenAnalysis(
    val originalWord: String,
    val bareWord: String,
    val explicitHaraka: String?,
    val contextLemma: String?,
    val contextStem: String?,
    val contextRoot: String?,
    val partOfSpeech: String,
    val caseOrMood: String?,
    val primary: MorphReading?,
    val readings: List<MorphReading>
)

data class RawSentenceAnalysis(
    val sentence: String,
    val layer2Used: Boolean,
    val tokens: List<RawTokenAnalysis>
)

data class WordIrabResult(
    val word: String,
    val diacritizedWord: String,
    val partOfSpeech: String,
    val syntacticRole: String,
    val grammaticalState: String,
    val fullIrab: String,
    val category: IrabCategory,
    val lemma: String,
    val root: String?,
    val stem: String,
    val pattern: String?,
    val subType: String?,
    val tense: String?,
    val gender: String?,
    val number: String?,
    val proclitics: List<AffixInfo>,
    val enclitics: List<AffixInfo>,
    val allReadings: List<MorphReading>
)

data class SentenceIrabResult(
    val id: Long = 0L,
    val sentence: String,
    val sentenceType: String,
    val words: List<WordIrabResult>,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val layer2Used: Boolean = false
)
