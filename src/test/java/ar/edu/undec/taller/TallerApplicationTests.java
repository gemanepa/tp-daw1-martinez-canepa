package ar.edu.undec.taller;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TallerApplicationTests {

	private final DataSource dataSource;

	@Autowired
	TallerApplicationTests(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Test
	void cargarContexto() {
	}

	@Test
	void usarBaseDeDatosEnMemoria() throws SQLException {
		try (var conexion = dataSource.getConnection()) {
			assertThat(conexion.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		}
	}

}
