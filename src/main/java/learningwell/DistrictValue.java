package learningwell;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class DistrictValue {
	private final List<String> districts;
	private final Double value;

	public DistrictValue(String district, Double value) {
		districts = new ArrayList<>();
		districts.add(district);
		this.value = value;
	}

	public Double getValue() {
		return value;
	}

	public void addDistrict(String district) {
		districts.add(district);
	}

	@Override
	public String toString() {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		String districtsString = districts.stream().collect(Collectors.joining(", "));
		return districtsString + " " + nf.format(value) + "%";
	}
}
