package flekos.chuletario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

/**
 * @author victor
 *
 */
public class MainApp {

	// Ejemplo de llamada desde línea de comandos
	// java -jar chuletario.jar --incluir_ddf --extension=xhtml --no_svg
	// --nodo_central=Ejemplo

	private final static String MOSTRAR_DDF = "--incluir_ddf";
	private final static String FILTRAR_EXTENSION = "--extension=";
	private final static String NO_GENERAR_SVG = "--no_svg";
	private final static String NODO_CENTRAL = "--nodo_central=";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		List<ApartadoDTO> listaFicheros = new ArrayList<>();

		String extension = "xhtml";
		String nodo = "nodo_central";

		boolean mostrarDdfDdt = false;
		boolean generarSvg = true;

		for (String arg : args) {
			if (StringUtils.containsIgnoreCase(arg, MOSTRAR_DDF)) {
				mostrarDdfDdt = true;
			} else if (StringUtils.containsIgnoreCase(arg, FILTRAR_EXTENSION)) {
				extension = StringUtils.remove(arg, FILTRAR_EXTENSION);
				if (StringUtils.isBlank(extension)) {
					extension = "xhtml";
					System.out.println("No se ha facilitado correctamente la extensión.");
				}
				System.out.println("Se buscarán ficheros con extensión: " + extension);
			} else if (StringUtils.containsIgnoreCase(arg, NODO_CENTRAL)) {
				nodo = StringUtils.remove(arg, NODO_CENTRAL);
				if (StringUtils.isBlank(nodo)) {
					nodo = "nodo_central";
				}
			} else if (StringUtils.containsIgnoreCase(arg, NO_GENERAR_SVG)) {
				generarSvg = false;
			}
		}

		try {

			// lanzamos la búsqueda
			List<String> files = findFiles(Paths.get(""), extension);

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

		StringBuffer sb = new StringBuffer();

		pintarMindMapArbol(listaFicheros, mostrarDdfDdt, sb, nodo, generarSvg);

		String nombreFichero = "Chuletario_" + nodo + ".puml";

		// generación del fichero puml
		generarFicheroPuml(sb, nombreFichero);

		// generación del fichero svg
		if (generarSvg) {
			generarFicheroSvg(sb, StringUtils.remove(nombreFichero, "puml") + "svg");
		}

	}

	/**
	 * Para un determinado fichero se generará un ApartadoDTO que representará un
	 * nodo del MindMap
	 *
	 * @param fichero        -> nombre del fichero en el que se buscarán las cadenas
	 * @param listaApartados -> lista de apartados al que se añade el nuevo apartado
	 *                       generado pare el fichero
	 */
	public static void searchMatchsInFile(String fichero, List<ApartadoDTO> listaApartados) {

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

			listaApartados.add(apartado);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Busca una cadena dentro de una fila de un fichero y si se encuentra una
	 * coincidencia se añade a la lista de coincidencias
	 *
	 * @param fila               -> la fila en la que se busca la cadena buscada
	 * @param cadenaBuscada      -> es el patrón de expresión regular a buscar
	 * @param listaCoincidencias -> coincidencias coincidencias encontradas
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
	 * Genera un nodo PUML
	 * 
	 * @param nivel         -> nivel del nodo
	 * @param nodoDto       -> apartado del nodo a pintar
	 * @param mostrarDdfDdt -> controla si se generarán los apartados de DDF y DDT
	 * @param sb            -> contenido PUML generado
	 */
	public static void pintarNodo(Integer nivel, ApartadoDTO nodoDto, boolean mostrarDdfDdt, StringBuffer sb) {

		String profundidad = new String(new char[nivel]).replace("\0", "*");
		boolean pagina = false;

		if (StringUtils.containsIgnoreCase(nodoDto.getRuta(), "include")) {
			sb.append(profundidad + ": <color #red><size:20><&paperclip></size> " + nodoDto.getArchivo()
					+ "</color> --> <color #blue> XXX </color>").append("\n");
		} else {
			sb.append(profundidad + ": <color #red>" + nodoDto.getArchivo() + "</color> --> <color #blue> XXX </color>")
					.append("\n");
			pagina = true;
		}

		sb.append(nodoDto.getRuta()).append("\n");

		sb.append("\t- <color #green>actions:</color>").append("\n");
		for (String cadena : nodoDto.getActions()) {
			sb.append("\t\t - " + cadena).append("\n");
		}

		sb.append("\t- <color #green>actions listeners: </color>").append("\n");
		for (String cadena : nodoDto.getActionListeners()) {
			sb.append("\t\t - " + cadena).append("\n");
		}

		sb.append("\t- <color #green>listMethods: </color>").append("\n");
		for (String cadena : nodoDto.getListMethod()) {
			sb.append("\t\t - " + cadena).append("\n");
		}

		sb.append("\t- <color #green>countMethods: </color>").append("\n");

		for (String cadena : nodoDto.getCountMethod()) {
			sb.append("\t\t - " + cadena).append("\n");
		}

		sb.append("\t- <color #green>includes: </color>").append("\n");
		for (String cadena : nodoDto.getIncludes()) {
			sb.append("\t\t - " + cadena).append("\n");
		}

		if (mostrarDdfDdt) {
			sb.append("----").append("\n");
			sb.append("\t- DDF").append("\n");
			sb.append("\t\t- Casos de uso:").append("\n");
			sb.append("----").append("\n");
			sb.append("\t- DDT").append("\n");
			sb.append("\t\t- Componente:").append("\n");
		}

		if (pagina) {
			sb.append(";").append("\n");
		} else {
			sb.append("<<rose>>;").append("\n");
		}

	}

	/**
	 * Crea un chuletario PUML con el nodo central y N ficheros PUML con los nodos
	 * hijos anidados
	 * 
	 * @param listaApartados -> lista de nodos encontrados a pintar
	 * @param mostrarDdfDdt  -> controla si se generarán los apartados DDF y DDT
	 * @param sb             -> contenido
	 * @param nodo           -> nombre del nodo central
	 * @param generarSvg     -> controla si se generarán los ficheros SVG
	 */
	public static void pintarMindMapArbol(List<ApartadoDTO> listaApartados, boolean mostrarDdfDdt, StringBuffer sb,
			String nodo, boolean generarSvg) {

		List<ApartadoDTO> listaNodosRaiz = new ArrayList<ApartadoDTO>();

		List<String> includes = new ArrayList<String>();

		for (ApartadoDTO item : listaApartados) {
			includes.addAll(item.getIncludes());
		}

		// para localizar los nodos raiz de nivel 2 -> **
		// el nodo central sería de nivel 1 -> *
		for (ApartadoDTO item : listaApartados) {
			boolean esRaiz = true;
			for (String include : includes) {
				if (StringUtils.containsIgnoreCase(include, item.getArchivo())) {
					esRaiz = false;
					break;
				}
			}
			if (esRaiz) {
				listaNodosRaiz.add(item);
			}
		}

		pintarCabeceraRaiz(sb, nodo);

//		int mitad = listaFicherosRaiz.size() / 2;
		int contador = 0;

		for (ApartadoDTO item : listaNodosRaiz) {
			String nombreFichero = StringUtils.leftPad("" + contador, 3, "0") + "_" + item.getArchivo() + ".puml";

			StringBuffer sbNodoRaiz = new StringBuffer();

			pintarCabeceraNodoSolitario(sbNodoRaiz, nodo);

			pintarHijosArbol(2, item, listaApartados, mostrarDdfDdt, sbNodoRaiz);

			pintarPie(sbNodoRaiz);

			// escribimos nodo hijo puml
			generarFicheroPuml(sbNodoRaiz, nombreFichero);

			// generamos fichero svg de nodo hijo puml
			if (generarSvg) {
				generarFicheroSvg(sbNodoRaiz, StringUtils.remove(nombreFichero, "puml") + "svg");
			}

			// creamos include del nodo hijo en el chuletario del nodo central
			sb.append("!include ").append(nombreFichero).append("\n");

			contador++;
		}

		pintarPie(sb);
	}

	/**
	 * Pinta un nodo y sus hijos de manera recursiva, anidando los hijos
	 * 
	 * @param nivel          -> nivel del nodo
	 * @param nodoDto        -> nodo a pintar y del que se pintarán sus hijos
	 * @param listaApartados -> lista de apartados usada para encontrar los hijos
	 *                       del nodoDto
	 * @param mostrarDdfDdt  -> controla si se tiene que pintar los apartados de DDF
	 *                       y DDT
	 * @param sb             -> contenido
	 */
	public static void pintarHijosArbol(Integer nivel, ApartadoDTO nodoDto, List<ApartadoDTO> listaApartados,
			boolean mostrarDdfDdt, StringBuffer sb) {

		pintarNodo(nivel, nodoDto, mostrarDdfDdt, sb);

		Integer nivelHijo = nivel + 1;

		for (String hijo : nodoDto.getIncludes()) {

			for (ApartadoDTO fichero : listaApartados) {
				if (StringUtils.containsIgnoreCase(hijo, "/" + fichero.getArchivo())) {
					pintarHijosArbol(nivelHijo, fichero, listaApartados, mostrarDdfDdt, sb);
				}
			}
		}

	}

	/**
	 * Genera los estilos a incluir en los ficheros PUML
	 * 
	 * @param sb -> contenido
	 */
	private static void pintarEstilos(StringBuffer sb) {
		sb.append("skin rose                        ").append("\n");
		sb.append("                                 ").append("\n");
		sb.append("<style>                          ").append("\n");
		sb.append("mindmapDiagram {                 ").append("\n");
		sb.append("  .blanco {                      ").append("\n");
		sb.append("    BackgroundColor white        ").append("\n");
		sb.append("  }                              ").append("\n");
		sb.append("  .green {                       ").append("\n");
		sb.append("    BackgroundColor lightgreen   ").append("\n");
		sb.append("  }                              ").append("\n");
		sb.append("  .rose {                        ").append("\n");
		sb.append("    BackgroundColor #FFBBCC      ").append("\n");
		sb.append("  }                              ").append("\n");
		sb.append("  .lightblue {                   ").append("\n");
		sb.append("    BackgroundColor #lightblue   ").append("\n");
		sb.append("  }                              ").append("\n");
		sb.append("  .orange {                      ").append("\n");
		sb.append("    BackgroundColor #orange      ").append("\n");
		sb.append("  }                              ").append("\n");
		sb.append("  .necesario_modificar {         ").append("\n");
		sb.append("'    BackgroundColor #f57e6c     ").append("\n");
		sb.append("  }                              ").append("\n");
		sb.append("}                                ").append("\n");
		sb.append("</style>                         ").append("\n");
	}

	/**
	 * Genera la cabecera y el nodo central del fichero PUML chuletario
	 * 
	 * @param sb          -> contenido
	 * @param nodoCentral -> nombre del nodo central
	 */
	public static void pintarCabeceraRaiz(StringBuffer sb, String nodoCentral) {
		sb.append("@startmindmap                    ").append("\n");
		sb.append("                                 ").append("\n");

		pintarEstilos(sb);

		sb.append("\n");
		sb.append("' variable creada para saber si se incluye o no la cabecera a la hora de visualizar").append("\n");
		sb.append("' los hijos o la chuleta con todos los hijos").append("\n");
		sb.append("!raiz = \"true\"").append("\n");
		sb.append("\n");

		sb.append("* ").append(nodoCentral).append("\n");
	}

	/**
	 * Genera el pie de un fichero PUML
	 * 
	 * @param sb -> contenido
	 */
	public static void pintarPie(StringBuffer sb) {
		sb.append("@endmindmap").append("\n");
	}

	/**
	 * Genera la cabecera de un nodo hijo de un fichero PUML
	 * 
	 * @param sb          -> contenido
	 * @param nodoCentral -> nombre del nodo central
	 */
	public static void pintarCabeceraNodoSolitario(StringBuffer sb, String nodoCentral) {
		sb.append("@startmindmap                    ").append("\n");
		sb.append("                                 ").append("\n");
		sb.append("!if (%not(%variable_exists(\"raiz\")))\n");

		pintarEstilos(sb);

		sb.append("* ").append(nodoCentral).append("\n");
		sb.append("       ").append("\n");
		sb.append("!endif ").append("\n");
	}

	/**
	 * Genera fichero PUML del MindMap
	 * 
	 * @param sb            -> contenido del fichero
	 * @param nombreFichero -> nombre del fichero generado
	 * 
	 */
	public static void generarFicheroPuml(StringBuffer sb, String nombreFichero) {
		File mapaPumlOutputFile = new File(nombreFichero);

		try (FileWriter fileWriter = new FileWriter(mapaPumlOutputFile)) {
			fileWriter.write(sb.toString());
			System.out.println("Archivo PUML generado: " + mapaPumlOutputFile.getAbsolutePath());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Genera fichero SVG del MindMap
	 * 
	 * @param sb            -> contenido del fichero
	 * @param nombreFichero -> nombre del fichero generado
	 * 
	 */
	public static void generarFicheroSvg(StringBuffer sb, String nombreFichero) {
		try {
			// Crear objeto SourceStringReader con el código PlantUML
			SourceStringReader reader = new SourceStringReader(sb.toString());

			// Crear archivo de salida SVG
			File outputFile = new File(nombreFichero);
			FileOutputStream outputStream = new FileOutputStream(outputFile);

			// Generar el archivo SVG
			String svg = reader.generateImage(outputStream, new FileFormatOption(FileFormat.SVG));

			// Cerrar el flujo de salida
			outputStream.close();

			// Imprimir la ruta del archivo SVG generado
			System.out.println("Archivo SVG generado: " + outputFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
