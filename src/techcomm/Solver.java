package techcomm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Solver {

	private static final long TIMEOUT = 10;

	private List<String> movies;
	private Map<String, List<String>> graph;
	private Map<String, List<String>> chains;

	private void insert(String key, String val, Map<String, List<String>> m) {
		if (!m.containsKey(key))
			m.put(key, new ArrayList<String>());
		List<String> l = m.get(key);
		l.add(val);
	}

	private static List<String> getPrefixes(String s) {
		List<String> prefixes = new ArrayList<String>();
		int end = s.length();
		while (true) {
			end = s.lastIndexOf(' ', end - 1);
			if (end < 0)
				break;
			String prefix = s.substring(0, end);
			prefixes.add(prefix);
		}
		return prefixes;
	}

	private static List<String> getSuffixes(String s) {
		List<String> suffixes = new ArrayList<String>();
		suffixes.add(s);
		int start = 0;
		while (true) {
			start = s.indexOf(' ', start + 1);
			if (start < 0)
				break;
			String suffix = s.substring(start);
			suffixes.add(suffix.trim());
		}
		return suffixes;
	}

	public Solver(List<String> movies) {
		this.movies = movies;
		graph = new HashMap<String, List<String>>();
		chains = new HashMap<String, List<String>>();

		Map<String, List<String>> prefixMap = new HashMap<String, List<String>>();

		for (String movie : movies) {
			graph.put(movie, new ArrayList<String>());
			for (String prefix : getPrefixes(movie))
				insert(prefix, movie, prefixMap);
		}

		for (String movie : movies) {
			List<String> children = graph.get(movie);
			for (String suffix : getSuffixes(movie))
				if (prefixMap.containsKey(suffix))
					children.addAll(prefixMap.get(suffix));
		}
	}

	public void randomizeGraph() {
		for (String m : movies) {
			List<String> children = graph.get(m);
			Collections.shuffle(children);
		}
	}

	public void go() {
		for (String movie : movies) {
			randomizeGraph();

			Set<String> empty = new LinkedHashSet<String>();
			Set<String> chain = findChain(movie, empty, TIMEOUT);
			chains.put(movie, new ArrayList<String>(chain));
			System.out.println(movie + ": " + chain.size());
		}

		List<String> goodMovies = new ArrayList<String>();
		for (String movie : movies) {
			List<String> chain = chains.get(movie);
			if (chain.size() > 10)
				goodMovies.add(movie);
		}

		try {
			for (int i = 1;; i++) {
				System.out.println(i + ": improve1");
				improve1(goodMovies);
				System.out.println(i + ": improve2");
				improve2(goodMovies);
				System.out.println(i + ": improve1");
				improve1(goodMovies);
				System.out.println(i + ": improve3");
				improve3(goodMovies, i * TIMEOUT);

				String outFile = "out" + i + "." + System.currentTimeMillis();
				ObjectOutputStream out = new ObjectOutputStream(
						new FileOutputStream(outFile));
				out.writeObject(chains);
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void improve1(List<String> movies) {
		for (int i = 0; i < 10; i++) {
			for (String movie : movies) {
				List<String> improved = improveChain(movie);
				setImproved(movie, improved);
			}
		}
	}

	private void improve2(List<String> movies) {
		for (String movie : movies) {
			List<String> bestChain = chains.get(movie);
			for (String m : movies) {
				List<String> chain = chains.get(m);
				bestChain = expand(bestChain, chain);
			}
			setImproved(movie, bestChain);
		}
	}

	private void improve3(List<String> movies, long timeout) {
		for (String movie : movies) {
			List<String> chain = chains.get(movie);
			for (int i = 0; i < 1; i++) {
				randomizeGraph();

				Set<String> c = new LinkedHashSet<String>(chain.subList(0, i));
				Set<String> s = findChain(chain.get(i), c, timeout);
				chain = expand(chain, new ArrayList<String>(s));
			}
			setImproved(movie, chain);
		}
	}

	private void setImproved(String movie, List<String> c) {
		if (c.size() > chains.get(movie).size()) {
			System.out.println(movie + ": " + chains.get(movie).size() + "->"
					+ c.size());
			chains.put(movie, c);
		}
	}

	private List<String> improveChain(String movie) {
		List<String> bestChain = new ArrayList<String>();

		List<String> children = graph.get(movie);
		if (children != null) {
			for (String child : children) {
				List<String> childChain = chains.get(child);
				int i = childChain.indexOf(movie);
				if (i == -1)
					i = childChain.size();
				if (i > bestChain.size())
					bestChain = new ArrayList<String>(childChain.subList(0, i));
			}
		}

		bestChain.add(0, movie);
		List<String> original = chains.get(movie);
		bestChain = original.size() > bestChain.size() ? original : bestChain;

		return bestChain;
	}

	private Set<String> findChain(String start, Set<String> chain, long time) {
		chain.add(start);

		if (time < 0)
			return chain;
		time += System.currentTimeMillis();

		List<String> children = graph.get(start);
		if (children != null) {
			Set<String> maxChain = chain;
			for (String movie : children) {
				if (!chain.contains(movie)) {
					Set<String> chainClone = new LinkedHashSet<String>(chain);
					Set<String> newChain = findChain(movie, chainClone, time
							- System.currentTimeMillis());
					if (newChain.size() > maxChain.size())
						maxChain = newChain;
				}
			}
			chain = maxChain;
		}

		return chain;
	}

	public static List<String> expand(List<String> x, List<String> y) {
		List<String> best = new ArrayList<String>();

		Map<String, Integer> a = new HashMap<String, Integer>();
		for (int i = 0; i < x.size(); i++)
			a.put(x.get(i), i);

		Map<String, Integer> b = new HashMap<String, Integer>();
		for (int i = 0; i < y.size(); i++)
			b.put(y.get(i), i);

		for (int i = 0; i < x.size(); i++) {
			String movie = x.get(i);
			Integer j = b.get(movie);
			if (j != null) {
				int j2 = j + 1;
				while (j2 < y.size() && !a.containsKey(y.get(j2)))
					j2++;
				if (j2 < y.size()) {
					int i2 = a.get(y.get(j2));
					if (i2 > i && j2 - j > i2 - i) {
						best.addAll(y.subList(j, j2));
						i = a.get(y.get(j2)) - 1;
						continue;
					}
				}
				if (j2 - j > x.size() - i) {
					best.addAll(y.subList(j, j2));
					break;
				}
			}
			best.add(movie);
		}

		return best;
	}

	public static void main(String[] args) {
		try {
			List<String> movies = new ArrayList<String>();
			Scanner scanner = new Scanner(new File("movies.lst"));
			while (scanner.hasNextLine()) {
				String movie = scanner.nextLine();
				movies.add(movie.trim());
			}

			Solver s = new Solver(movies);
			s.go();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
