package flekos.chuletario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

		List<ApartadoDTO> listaFicheros = new ArrayList<>();

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

			// lanzamos la búsqueda
			files = findFiles(Paths.get(""), extension);
			if (files.isEmpty()) {
				System.out.println("No se han encontrado ficheros");
			} else {
				files.forEach(x -> {
					searchMatchsInFile(x, listaFicheros);
					//System.out.println("");
				});
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		System.out.println("@startmindmap");
		System.out.println("* nodo padre");
		for(ApartadoDTO item: listaFicheros) {
			System.out.println("**: " + item.getArchivo());
			System.out.println("\t- actions");
			for(String cadena: item.getActions()) {
				System.out.println("\t\t - " + cadena);
			}
			
			System.out.println("\t- actions listeners");
			for(String cadena: item.getActionListeners()) {
				System.out.println("\t\t - " + cadena);
			}
			
			System.out.println("\t- listMethods");
			for(String cadena: item.getListMethod()) {
				System.out.println("\t\t - " + cadena);
			}
			
			System.out.println("\t- countMethods");
			for(String cadena: item.getCountMethod()) {
				System.out.println("\t\t - " + cadena);
			}
			
			System.out.println("\t- includes");
			for(String cadena: item.getIncludes()) {
				System.out.println("\t\t - " + cadena);
			}
			System.out.println(";");
		}
		System.out.println("@endmindmap");
		
	}

	/**
	 * Inicializa la estructura de apartados y cadenas buscadas
	 * 
	 * @param apartados
	 * @param apartado      -> apartado que agrupa los resultados coincidentes con
	 *                      la cadena buscada
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
	public static void searchMatchsInFile(String fichero, List<ApartadoDTO> listaFicheros) {
		//System.out.println("Fichero -> " + fichero);
		BufferedReader reader;

		try {

			File archivo = new File(fichero);

			ApartadoDTO apartado = new ApartadoDTO();
			apartado.setArchivo(archivo.getName());
			apartado.setRuta(archivo.getAbsolutePath());

			reader = new BufferedReader(new FileReader(archivo));
			String line = reader.readLine();

			while (line != null) {
				final String fila = line;

				searchString(fila, "action=\"", apartado.getActions());
				searchString(fila, "actionlistener=\"", apartado.getActionListeners());
				searchString(fila, "<ui:include", apartado.getIncludes());
				searchString(fila, "listMethod=\"", apartado.getListMethod());
				searchString(fila, "countMethod=\"", apartado.getListMethod());

				// read next line
				line = reader.readLine();

			}

			reader.close();

			listaFicheros.add(apartado);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Busca una cadena dentro de una fila y si se encuentra coincidencia se añade a
	 * lista de coincidencias
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
