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

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String extension = "xhtml";

		if (args != null && args.length == 1) {
			extension = args[0];
			System.out.println("Se buscarán ficheros con extensión: " + extension);
		} else if (args == null || args.length != 1) {
			System.err.println(
					"No se han facilitado los parámetros correctos. Se buscarán ficheros con extensión: " + extension);
		}

		List<String> files;
		try {

			/*
			 * primero definimos la estructura, en la que definimos los apartados que vamos
			 * a buscar, el patrón para cada apartado y la lista de coincidencias que se
			 * obtienen
			 * 
			 */

			HashMap<String, HashMap<String, List<String>>> apartadosMap = new HashMap<>();
			initializeApartados(apartadosMap, "** acciones **", "action=\"");
			initializeApartados(apartadosMap, "** acciones_listener **", "actionlistener=\"");
			initializeApartados(apartadosMap, "** includes **", "<ui:include");
			initializeApartados(apartadosMap, "** listas_obtenerLista **", "listMethod=\"");
			initializeApartados(apartadosMap, "** listas_obtenerTotal **", "countMethod=\"");

			// lanzamos la búsqueda
			files = findFiles(Paths.get(""), extension);
			if (files.isEmpty()) {
				System.out.println("No se han encontrado ficheros");
			} else {
				files.forEach(x -> {
					searchMatchsInFile(x, apartadosMap);
					System.out.println("");
				});
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Inicializa la estructura de apartados y cadenas buscadas
	 * 
	 * @param apartados 
	 * @param apartado -> apartado que agrupa los resultados coincidentes con la cadena buscada
	 * @param cadenaBuscada -> cadena buscada 
	 */
	public static void initializeApartados(HashMap<String, HashMap<String, List<String>>> apartados, String apartado,
			String cadenaBuscada) {
		if (apartados == null) {
			apartados = new HashMap<>();
		}
		if (StringUtils.isNotBlank(apartado) && StringUtils.isNotBlank(cadenaBuscada)) {
			HashMap<String, List<String>> busquedaMap = new HashMap<>();
			busquedaMap.put(cadenaBuscada, new ArrayList<String>());
			apartados.put(apartado, busquedaMap);
		}
	}

	/**
	 * Busca una serie de cadenas dentro de un determinado fichero y muestra las
	 * coincidencias
	 * 
	 * @param fichero -> nombre del fichero en el que se buscarán las cadenas
	 */
	public static void searchMatchsInFile(String fichero, HashMap<String, HashMap<String, List<String>>> apartadosMap) {
		System.out.println("Fichero -> " + fichero);
		BufferedReader reader;

		try {
			reader = new BufferedReader(new FileReader(fichero));
			String line = reader.readLine();

			while (line != null) {
				final String fila = line;
				apartadosMap.forEach((key, value) -> {
					HashMap<String, List<String>> busquedaMap = value;
					busquedaMap.forEach((keyMap, valueMap) -> {
						searchString(fila, keyMap, valueMap);
					});
				});

				// read next line
				line = reader.readLine();

			}

			reader.close();

			apartadosMap.forEach((key, value) -> {
				System.out.println("\n\t" + key+"\n");
				HashMap<String, List<String>> busquedaMap = value;
				busquedaMap.forEach((keyMap, valueMap) -> {
					for (String item : valueMap) {
						System.out.println("\t\t" + item);
					}
					;
				});

			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Busca una cadena dentro de una fila y si se encuentra coincidencia se añade a lista de 
	 * coincidencias
	 * 
	 * @param linea
	 * @param cadenaBuscada
	 * @param listaCoincidencias
	 */
	public static void searchString(String fila, String cadenaBuscada, List<String> listaCoincidencias) {
		if (StringUtils.containsIgnoreCase(fila, cadenaBuscada)) {
			listaCoincidencias.add(StringUtils.trim(fila));
		}
	}

	/**
	 * Realiza la búsqueda de ficheros que tengan una determinada extensión
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
