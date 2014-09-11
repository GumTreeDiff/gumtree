package fr.labri.gumtree.client.batch;

import static org.junit.Assert.*;

import org.junit.Test;

public class AnnotationsTest {
	@Test
	public void testOne() throws Exception {
		assertEquals(1,2);
	}
	
	@Test
	public void testTwo() throws Exception {
		assertEquals(1,2);
		assertTrue(true);
	}
}
