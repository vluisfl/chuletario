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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * @author victor
 *
 */
public class MainApp {

	// chuletario --incluir_ddt=n --extension=java

	private final static String NO_MOSTRAR_DDT = "--incluir_ddt=n";
	private final static String FILTRAR_EXTENSION = "--extension=";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		List<ApartadoDTO> listaFicheros = new ArrayList<>();

		String extension = "xhtml";

		boolean mostrarDdfDdt = true;

		for (String arg : args) {
			if (StringUtils.containsIgnoreCase(arg, NO_MOSTRAR_DDT)) {
				mostrarDdfDdt = false;
			} else if (StringUtils.containsIgnoreCase(arg, FILTRAR_EXTENSION)) {
				extension = StringUtils.remove(arg, FILTRAR_EXTENSION);
				if (StringUtils.isBlank(extension)) {
					extension = "xhtml";
					System.out.println("No se ha facilitado correctamente la extensión.");
				}
				System.out.println("Se buscarán ficheros con extensión: " + extension);
			}
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
					// System.out.println("");
				});
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// pintarMindMapPlano(listaFicheros);

		pintarMindMapArbol(listaFicheros, mostrarDdfDdt);

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
		// System.out.println("Fichero -> " + fichero);
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

				searchString(fila, ".*action=\".*", apartado.getActions());
				searchString(fila, ".*actionlistener=\".*", apartado.getActionListeners());
				searchString(fila, ".*src=.*html\".*", apartado.getIncludes());
				searchString(fila, ".*listMethod=\".*", apartado.getListMethod());
				searchString(fila, ".*countMethod=\".*", apartado.getCountMethod());

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
	 * @param fila               la fila en la que se busca la cadena buscada
	 * @param cadenaBuscada      es el patrón de expresión regular a buscar
	 * @param listaCoincidencias
	 */
	public static void searchString(String fila, String cadenaBuscada, List<String> listaCoincidencias) {

		if (fila.matches(cadenaBuscada)) {
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

	/**
	 * pinta el modelo de mindmap plano
	 *
	 * @param listaFicheros
	 * @param mostrarDdfDdt
	 */
	public static void pintarMindMapPlano(List<ApartadoDTO> listaFicheros, boolean mostrarDdfDdt) {
		System.out.println("@startmindmap");
		System.out.println("* nodo padre");
		for (ApartadoDTO item : listaFicheros) {
			pintarNodo(2, item, mostrarDdfDdt);
		}
		System.out.println("@endmindmap");
	}

	/**
	 * @param nivel
	 * @param itemDTO
	 */
	public static void pintarNodo(Integer nivel, ApartadoDTO itemDTO, boolean mostrarDdfDdt) {

		String profundidad = new String(new char[nivel]).replace("\0", "*");

		if (StringUtils.containsIgnoreCase(itemDTO.getRuta(), "include")) {
			System.out.println(profundidad + ": <color #red><size:20><&paperclip></size> " + itemDTO.getArchivo()
					+ "</color> --> <color #blue> XXX </color>");
		} else {
			System.out.println(
					profundidad + ": <color #red>" + itemDTO.getArchivo() + "</color> --> <color #blue> XXX </color>");
		}

		System.out.println("\t- <color #green>actions:</color>");
		for (String cadena : itemDTO.getActions()) {
			System.out.println("\t\t - " + cadena);
		}

		System.out.println("\t- <color #green>actions listeners: </color>");
		for (String cadena : itemDTO.getActionListeners()) {
			System.out.println("\t\t - " + cadena);
		}

		System.out.println("\t- <color #green>listMethods:</color>");
		for (String cadena : itemDTO.getListMethod()) {
			System.out.println("\t\t - " + cadena);
		}

		System.out.println("\t- <color #green>countMethods:</color>");
		for (String cadena : itemDTO.getCountMethod()) {
			System.out.println("\t\t - " + cadena);
		}

		System.out.println("\t- <color #green>includes:</color>");
		for (String cadena : itemDTO.getIncludes()) {
			System.out.println("\t\t - " + cadena);
		}

		if (mostrarDdfDdt) {

			System.out.println("----");
			System.out.println("\t- DDF");
			System.out.println("\t\t- Casos de uso:");
			System.out.println("----");
			System.out.println("\t- DDT");
			System.out.println("\t\t- Componente:");
			System.out.println(";");
		}
	}

	public static void pintarMindMapArbol(List<ApartadoDTO> listaFicheros, boolean mostrarDdfDdt) {

		List<ApartadoDTO> listaFicherosRaiz = new ArrayList<ApartadoDTO>();

		List<String> includes = new ArrayList<String>();

		for (ApartadoDTO item : listaFicheros) {
			includes.addAll(item.getIncludes());
		}

		for (ApartadoDTO item : listaFicheros) {
			boolean esRaiz = true;
			for (String include : includes) {
				if (StringUtils.containsIgnoreCase(include, item.getArchivo())) {
					esRaiz = false;
					break;
				}
			}
			if (esRaiz) {
				listaFicherosRaiz.add(item);
			}

		}

		System.out.println("@startmindmap");
		System.out.println("* nodo padre");
		for (ApartadoDTO item : listaFicherosRaiz) {
			pintarHijosArbol(2, item, listaFicheros, mostrarDdfDdt);
		}
		System.out.println("@endmindmap");
	}

	/**
	 * @param nivel
	 * @param padre
	 * @param listaFicheros
	 */
	public static void pintarHijosArbol(Integer nivel, ApartadoDTO padre, List<ApartadoDTO> listaFicheros,
			boolean mostrarDdfDdt) {

		pintarNodo(nivel, padre, mostrarDdfDdt);

		Integer nivelHijo = nivel + 1;

		for (String hijo : padre.getIncludes()) {

			for (ApartadoDTO fichero : listaFicheros) {
				if (StringUtils.containsIgnoreCase(hijo, "/" + fichero.getArchivo())) {
					pintarHijosArbol(nivelHijo, fichero, listaFicheros, mostrarDdfDdt);
				}
			}
		}

	}
}
