package craft3dmodern.test;

import java.util.ArrayList;
import java.util.List;

import craft3dmodern.client.font.FontRenderer;

/** Headless test: metryki fontu z prawdziwych assetow 26.2 (bez GL). */
public final class FontTest {
    private static final List<String> FAILS = new ArrayList<String>();

    private static void check(boolean ok, String what) {
        if (ok) System.out.println("ok:   " + what);
        else FAILS.add(what);
    }

    public static void main(String[] args) throws Exception {
        FontRenderer f = FontRenderer.load();

        check(f.has('A') && f.has('a') && f.has('0') && f.has('.'), "basic ascii glyphs present");
        check(f.width("A") > 0 && f.width("i") > 0, "glyph advances > 0");

        // proporcjonalnosc: "iiiii" wezsze niz "MMMMM"
        check(f.width("iiiii") < f.width("MMMMM"), "proportional (iiiii < MMMMM)");

        // spacja: przesuwa kursor (adv > 0), bez tekstury
        check(f.width("A A") > f.width("AA"), "space advances cursor");

        // polskie znaki
        String pl = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ";
        for (int i = 0; i < pl.length(); i++) {
            char c = pl.charAt(i);
            check(f.has(c), "polish char " + c + " present");
        }
        check(f.width("żółw") > 0 && f.width("Zółw") > 0, "polish word width ok");

        // ciag: szerokosc rosnie z dlugoscia
        check(f.width("Hello") > f.width("Hell"), "width grows with length");

        // wszystkie klucze tlumaczen ktore rysujemy maja znaki w foncie
        String sample = "Tryb jednoosobowy Wybierz świat Opcje Gra Język Polski English";
        float w = f.width(sample);
        check(w > 0, "translation sample measurable (" + w + ")");

        if (FAILS.isEmpty()) {
            System.out.println("ALL FONT TESTS PASSED (OK)");
        } else {
            System.out.println(FAILS.size() + " FAILURES");
            for (String s : FAILS) System.out.println("  FAIL " + s);
            System.exit(1);
        }
    }
}
