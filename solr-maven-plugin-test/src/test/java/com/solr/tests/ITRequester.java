package com.solr.tests;

import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;

@ThreadLeakScope(Scope.NONE)
public class ITRequester extends RandomizedTest {

	private static Requester requester;

	@BeforeClass
	public static void initTest() {
		requester = new Requester("testCol", 8887);
	}
	
	@Test
	public void testPing() throws Exception {
		requester.ping();
	}
	
	@Test
	public void testAdd() throws Exception {
		//'name' field is only defined in 'solr-it-conf'
		//we're always adding a doc with the same id, so the index will not grow
		requester.addDoc_WithName();
	}
}
