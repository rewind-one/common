package one.rewind.txt.test;

import one.rewind.txt.distance.Levenshtein;
import one.rewind.txt.distance.NeedlemanWunsch;
import org.junit.Test;

public class TextTest {

	@Test
	public void needlemanWunschTest() {

		char[] seqA = "苹果 iPhone6 金色 64G 国行 自用苹果6，金色，国行，64G".toCharArray();
		char[] seqB = "iPhone 6 A1586 公开版".toCharArray();

		NeedlemanWunsch nw = new NeedlemanWunsch();
		nw.init(seqA, seqB);
		nw.process();
		nw.backtrack();

		nw.printMatrix();
		nw.printScoreAndAlignments();
	}

	@Test
	public void levenshteinTest(){

		String str = "苹果 iPhone6 金色 64G 国行 自用苹果6，金色，国行，64G";
		String target = "iPhone 6 A1586 公开版";

		System.out.println("similarityRatio: "
				+ Levenshtein.compare(str, target));

	}

	@Test
	public void splitTest() {
		String str = "https://aaa/adsf/sdf";
		for(String s: str.split("/", 4)) {
			System.err.println(s);
		}
	}
}
