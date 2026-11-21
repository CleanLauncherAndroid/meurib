package com.example

import com.example.engine.ArabicGrammarEngine
import com.example.model.AffixInfo
import com.example.model.MorphReading
import com.example.model.RawSentenceAnalysis
import com.example.model.RawTokenAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicGrammarEngineTest {

    private fun reading(
        pos: String,
        subType: String? = null,
        tense: String? = null,
        caseOrMood: String? = null,
        number: String? = null,
        gender: String? = null,
        voice: String? = null,
        root: String? = null,
        proclitics: List<AffixInfo> = emptyList()
    ) = MorphReading(
        diacritized = "",
        lemma = "",
        stem = "",
        root = root,
        pattern = null,
        partOfSpeech = pos,
        subType = subType,
        tense = tense,
        gender = gender,
        number = number,
        person = null,
        pronoun = null,
        voice = voice,
        state = null,
        caseOrMood = caseOrMood,
        proclitics = proclitics,
        enclitics = emptyList(),
        score = 1.0
    )

    private fun token(
        original: String,
        bare: String = ArabicGrammarEngine.stripDiacritics(original),
        explicitHaraka: String? = null,
        primary: MorphReading,
        readings: List<MorphReading> = listOf(primary)
    ) = RawTokenAnalysis(
        originalWord = original,
        bareWord = bare,
        explicitHaraka = explicitHaraka,
        contextLemma = null,
        contextStem = null,
        contextRoot = primary.root,
        partOfSpeech = primary.partOfSpeech,
        caseOrMood = explicitHaraka ?: primary.caseOrMood,
        primary = primary,
        readings = readings
    )

    @Test
    fun verbalSentence_assignsVerbSubjectObject() {
        val raw = RawSentenceAnalysis(
            sentence = "كَتَبَ الطَّالِبُ الدَّرْسَ",
            layer2Used = false,
            tokens = listOf(
                token("كَتَبَ", "كتب", null, reading("فعل", tense = "ماض")),
                token("الطَّالِبُ", "الطالب", "مرفوع", reading("اسم", caseOrMood = "مرفوع")),
                token("الدَّرْسَ", "الدرس", "منصوب", reading("اسم", caseOrMood = "منصوب"))
            )
        )

        val result = ArabicGrammarEngine.analyzeSentence(raw)
        assertEquals("جملة فعلية", result.sentenceType)
        assertEquals("فعل ماضٍ", result.words[0].syntacticRole)
        assertEquals("فاعل", result.words[1].syntacticRole)
        assertEquals("مرفوع وعلامة رفعه الضمة الظاهرة", result.words[1].grammaticalState)
        assertEquals("مفعول به", result.words[2].syntacticRole)
        assertEquals("منصوب وعلامة نصبه الفتحة الظاهرة", result.words[2].grammaticalState)
    }

    @Test
    fun passiveVerb_assignsNaibFail() {
        val raw = RawSentenceAnalysis(
            sentence = "كُتِبَ الدَّرْسُ",
            layer2Used = false,
            tokens = listOf(
                token("كُتِبَ", "كتب", null, reading("فعل", tense = "ماض", voice = "مجهول")),
                token("الدَّرْسُ", "الدرس", "مرفوع", reading("اسم", caseOrMood = "مرفوع"))
            )
        )

        val result = ArabicGrammarEngine.analyzeSentence(raw)
        assertEquals("فعل ماضٍ مبني للمجهول", result.words[0].syntacticRole)
        assertEquals("نائب فاعل", result.words[1].syntacticRole)
    }

    @Test
    fun fiveVerbs_assignsThubutAlNun() {
        val raw = RawSentenceAnalysis(
            sentence = "الطلاب يكتبون الدرس",
            layer2Used = false,
            tokens = listOf(
                token("الطلاب", "الطلاب", null, reading("اسم", caseOrMood = "مرفوع")),
                token("يكتبون", "يكتبون", null, reading("فعل", tense = "مضارع")),
                token("الدرس", "الدرس", null, reading("اسم", caseOrMood = "منصوب"))
            )
        )

        val result = ArabicGrammarEngine.analyzeSentence(raw)
        assertTrue(result.words[1].grammaticalState.contains("ثبوت النون"))
    }

    @Test
    fun nominalSentence_assignsMubtadaAndKhabar() {
        val raw = RawSentenceAnalysis(
            sentence = "العلمُ نورٌ",
            layer2Used = false,
            tokens = listOf(
                token("العلمُ", "العلم", "مرفوع", reading("اسم", caseOrMood = "مرفوع")),
                token("نورٌ", "نور", "مرفوع", reading("اسم", caseOrMood = "مرفوع"))
            )
        )

        val result = ArabicGrammarEngine.analyzeSentence(raw)
        assertEquals("جملة اسمية", result.sentenceType)
        assertEquals("مبتدأ", result.words[0].syntacticRole)
        assertEquals("خبر", result.words[1].syntacticRole)
    }

    @Test
    fun prepositionalPhrase_assignsPrepositionAndMajrour() {
        val raw = RawSentenceAnalysis(
            sentence = "ذهب الولد إلى المدرسة",
            layer2Used = false,
            tokens = listOf(
                token("ذهب", "ذهب", null, reading("فعل", tense = "ماض")),
                token("الولد", "الولد", null, reading("اسم", caseOrMood = "مرفوع")),
                token("إلى", "إلى", null, reading("حرف", subType = "جر")),
                token("المدرسة", "المدرسة", null, reading("اسم", caseOrMood = "مجرور"))
            )
        )

        val result = ArabicGrammarEngine.analyzeSentence(raw)
        assertEquals("جملة فعلية", result.sentenceType)
        assertEquals("فاعل", result.words[1].syntacticRole)
        assertEquals("حرف جر", result.words[2].syntacticRole)
        assertEquals("اسم مجرور", result.words[3].syntacticRole)
        assertEquals("مجرور وعلامة جره الكسرة الظاهرة", result.words[3].grammaticalState)
    }
}
