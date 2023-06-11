package flekos.buscador_texto;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * @author victor
 *
 */
public class MainApp {

	public static void main(String[] args) {

		List<String> files;
		try {
			files = findFiles(Paths.get(""), "xhtml");
			if (files.isEmpty()) {
				System.out.println("No se han encontrado ficheros");
			} else {
				files.forEach(x -> {
					parsearFichero(x);
					System.out.println("");
				});
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void parsearFichero(String fichero) {
		System.out.println("Fichero -> " + fichero);
		BufferedReader reader;

		try {
			reader = new BufferedReader(new FileReader(fichero));
			String line = reader.readLine();

			HashMap<String, List<String>> datosParseados = new HashMap<>();
			datosParseados.put("acciones", new ArrayList<>());
			datosParseados.put("acciones_listener", new ArrayList<>());
			datosParseados.put("includes", new ArrayList<>());
			datosParseados.put("listas_obtenerLista", new ArrayList<>());
			datosParseados.put("listas_obtenerTotal", new ArrayList<>());

			while (line != null) {
				// read next line
				line = reader.readLine();

				parsearLinea(line, "action=\"", datosParseados.get("acciones"));
				parsearLinea(line, "actionlistener=\"", datosParseados.get("acciones_listener"));
				parsearLinea(line, "ui:include", datosParseados.get("includes"));
				parsearLinea(line, "listMethod=\"", datosParseados.get("listas_obtenerLista"));
				parsearLinea(line, "countMethod=\"", datosParseados.get("listas_obtenerTotal"));

			}

			reader.close();

			datosParseados.forEach((key, value) -> {
				System.out.println("\t" + key);
				for (String item : value) {
					System.out.println("\t\t" + item);
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void parsearLinea(String linea, String cadenaBuscada, List<String> lista) {
		if (StringUtils.containsIgnoreCase(linea, cadenaBuscada)) {
			lista.add(StringUtils.trim(linea));
		}
	}

	/**
	 * 
	 * @param path
	 * @param fileExtension
	 * @return
	 * @author https://mkyong.com/java/how-to-find-files-with-certain-extension-only/
	 * @throws IOException
	 */
	public static List<String> findFiles(Path path, String fileExtension) throws IOException {

		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Path must be a directory!");
		}

		List<String> result;

		try (Stream<Path> walk = Files.walk(path)) {
			result = walk.filter(p -> !Files.isDirectory(p))
					// this is a path, not string,
					// this only test if path end with a certain path
					// .filter(p -> p.endsWith(fileExtension))
					// convert path to string first
					// .map(p -> p.toAbsolutePath().toString().toLowerCase())
					.map(p -> p.toAbsolutePath().toString()).filter(f -> f.endsWith(fileExtension))
					.filter(f -> !f.contains("target")).collect(Collectors.toList());
		}

		return result;
	}

}
