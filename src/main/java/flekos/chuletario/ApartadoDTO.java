package flekos.chuletario;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ApartadoDTO {

	private String archivo;
	
	private String ruta;
	
	
	private String bean;
	
	private List<String> includes = new ArrayList<>();
	
	private List<String> actions = new ArrayList<>();
	
	private List<String> actionListeners = new ArrayList<>();
	
	private List<String> countMethod = new ArrayList<>();
	
	private List<String> listMethod = new ArrayList<>();
	
	private List<ApartadoDTO> padres = new ArrayList<>();
	
	
}
