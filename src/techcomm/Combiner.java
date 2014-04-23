package techcomm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Combiner {

	// private static final long TIMEOUT = 10;
	//
	// private List<String> movies;
	private Map<String, List<String>> graph;
	private Map<String, List<String>> chains1;
	private Map<String, List<String>> chains2;

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

	public Combiner(List<String> movies) {
		// this.movies = movies;
		graph = new HashMap<String, List<String>>();

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

	private void improve1(List<String> movies) {
		for (String movie : movies) {
			List<String> improved = improveChain(movie);
			setImproved(movie, improved);
		}
	}

	private void improve2(List<String> movies) {
		for (String movie : movies) {
			List<String> bestChain = chains1.get(movie);
			for (String m : movies) {
				List<String> chain = chains2.get(m);
				bestChain = expand(bestChain, chain);
			}
			setImproved(movie, bestChain);
		}
	}

	private List<String> improveChain(String movie) {
		List<String> bestChain = new ArrayList<String>();

		List<String> children = graph.get(movie);
		if (children != null) {
			for (String child : children) {
				List<String> childChain = chains2.get(child);
				int i = childChain.indexOf(movie);
				if (i == -1)
					i = childChain.size();
				if (i > bestChain.size())
					bestChain = new ArrayList<String>(childChain.subList(0, i));
			}
		}

		bestChain.add(0, movie);
		List<String> original = chains1.get(movie);
		bestChain = original.size() > bestChain.size() ? original : bestChain;

		return bestChain;
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

	private void setImproved(String movie, List<String> c) {
		if (c.size() > chains1.get(movie).size()) {
			System.out.println(movie + ": " + chains1.get(movie).size() + "->"
					+ c.size());
			chains1.put(movie, c);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, List<String>> loadChains(String file)
			throws Exception {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
		Map<String, List<String>> chains = (Map<String, List<String>>) in
				.readObject();
		in.close();
		return chains;
	}

	public static void main(String[] args) {
		try {
			Map<String, List<String>> chains1 = loadChains("c.ser");
			Map<String, List<String>> chains2 = loadChains("d.ser");

			List<String> movies = new ArrayList<String>();
			Scanner scanner = new Scanner(new File("movies.lst"));
			while (scanner.hasNextLine()) {
				String movie = scanner.nextLine();
				movies.add(movie.trim());
			}

			Combiner l = new Combiner(movies);
			l.chains1 = chains1;
			l.chains2 = chains2;
			l.improve1(movies);
			l.improve2(movies);
			l.improve1(movies);

			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream("c.ser"));
			out.writeObject(chains1);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
