package com.example.treasure_and_battle.utils;

import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class HtmlRenderUtilsTest {

    private TextView textView;

    @Before
    public void setUp() {
        textView = new TextView(org.robolectric.RuntimeEnvironment.getApplication());
    }

    @Test
    public void testSetHtmlText_NullContent() {
        HtmlRenderUtils.setHtmlText(textView, null);
        assertEquals("", textView.getText().toString());
    }

    @Test
    public void testSetHtmlText_EmptyContent() {
        HtmlRenderUtils.setHtmlText(textView, "");
        assertEquals("", textView.getText().toString());
    }

    @Test
    public void testSetHtmlText_PlainText() {
        HtmlRenderUtils.setHtmlText(textView, "Hello World");
        assertEquals("Hello World", textView.getText().toString());
    }

    @Test
    public void testSetHtmlText_BoldTag() {
        HtmlRenderUtils.setHtmlText(textView, "<b>Bold</b> text");
        String result = textView.getText().toString();
        assertTrue(result.contains("Bold"));
        assertTrue(result.contains("text"));
    }

    @Test
    public void testSetHtmlText_ColorTag() {
        HtmlRenderUtils.setHtmlText(textView, "<font color=\"#FF0000\">Red</font> text");
        String result = textView.getText().toString();
        assertTrue(result.contains("Red"));
        assertTrue(result.contains("text"));
    }

    @Test
    public void testSetHtmlText_NewlineConvertedToBr() {
        HtmlRenderUtils.setHtmlText(textView, "Line 1\nLine 2\nLine 3");
        String result = textView.getText().toString();
        // 换行符应该被转换为<br>
        assertTrue(result.contains("Line 1"));
        assertTrue(result.contains("Line 2"));
        assertTrue(result.contains("Line 3"));
    }

    @Test
    public void testSetHtmlText_MultipleNewlines() {
        HtmlRenderUtils.setHtmlText(textView, "A\nB\nC\nD");
        String result = textView.getText().toString();
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
        assertTrue(result.contains("C"));
        assertTrue(result.contains("D"));
    }

    @Test
    public void testSetHtmlText_ComplexHtml() {
        HtmlRenderUtils.setHtmlText(textView, "<b>Bold</b> and <i>italic</i>");
        String result = textView.getText().toString();
        assertTrue(result.contains("Bold"));
        assertTrue(result.contains("and"));
        assertTrue(result.contains("italic"));
    }

    @Test
    public void testSetHtmlText_BrTagDirectly() {
        HtmlRenderUtils.setHtmlText(textView, "Line 1<br>Line 2");
        String result = textView.getText().toString();
        assertTrue(result.contains("Line 1"));
        assertTrue(result.contains("Line 2"));
    }

    @Test
    public void testColorToHex_Red() {
        String hex = HtmlRenderUtils.colorToHex(0xFF0000);
        assertEquals("#FF0000", hex);
    }

    @Test
    public void testColorToHex_Green() {
        String hex = HtmlRenderUtils.colorToHex(0x00FF00);
        assertEquals("#00FF00", hex);
    }

    @Test
    public void testColorToHex_Blue() {
        String hex = HtmlRenderUtils.colorToHex(0x0000FF);
        assertEquals("#0000FF", hex);
    }

    @Test
    public void testColorToHex_White() {
        String hex = HtmlRenderUtils.colorToHex(0xFFFFFF);
        assertEquals("#FFFFFF", hex);
    }

    @Test
    public void testColorToHex_Black() {
        String hex = HtmlRenderUtils.colorToHex(0x000000);
        assertEquals("#000000", hex);
    }

    @Test
    public void testColorToHex_WithAlpha() {
        // Alpha 位应该被忽略
        String hex = HtmlRenderUtils.colorToHex(0x80FF0000); // 半透明红色
        assertEquals("#FF0000", hex);
    }

    @Test
    public void testColorToHex_Gray() {
        String hex = HtmlRenderUtils.colorToHex(0x808080);
        assertEquals("#808080", hex);
    }

    @Test
    public void testColorToHex_Zero() {
        String hex = HtmlRenderUtils.colorToHex(0);
        assertEquals("#000000", hex);
    }

    @Test
    public void testColorToHex_MaxInt() {
        String hex = HtmlRenderUtils.colorToHex(0xFFFFFFFF);
        assertEquals("#FFFFFF", hex);
    }

    @Test
    public void testColorToHex_SixDigitFormat() {
        String hex = HtmlRenderUtils.colorToHex(0xABCDEF);
        // 确保是6位十六进制
        assertEquals(7, hex.length()); // # + 6位
        assertTrue(hex.startsWith("#"));
    }
}
