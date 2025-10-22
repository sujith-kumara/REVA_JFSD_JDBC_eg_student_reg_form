import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server {

    public static void main(String[] args) throws Exception {

        // 🧩 Step 1: Register the Driver class
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Start a basic HTTP server
        ServerSocket server = new ServerSocket(8080);
        System.out.println("🚀 Server running at http://localhost:8080");

        while (true) {
            try {
                Socket socket = server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                if (line == null) continue;

                if (line.startsWith("POST")) {
                    handlePost(socket, in);
                } else if (line.startsWith("GET")) {
                    // Check if this is a request for static files
                    if (line.contains("style.css")) {
                        handleCSS(socket);
                    } else if (line.contains("script.js")) {
                        handleJS(socket);
                    } else {
                        handleGet(socket);
                    }
                }

                socket.close();
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
            }
        }
    }

    // Handle POST requests (Add, Update, Delete)
    private static void handlePost(Socket socket, BufferedReader in) throws Exception {
        int length = 0;
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:"))
                length = Integer.parseInt(line.split(":")[1].trim());
        }

        char[] body = new char[length];
        in.read(body);
        String data = new String(body);

        if (data.contains("deleteId=")) {
            int id = Integer.parseInt(data.split("=")[1]);
            deleteStudent(id);
        } else if (data.contains("updateId=")) {
            String[] parts = data.split("&");
            int id = Integer.parseInt(parts[0].split("=")[1]);
            String name = URLDecoder.decode(parts[1].split("=")[1], "UTF-8");
            String srn = URLDecoder.decode(parts[2].split("=")[1], "UTF-8");
            String newSrn = URLDecoder.decode(parts[3].split("=")[1], "UTF-8"); // New SRN parameter
            updateStudent(id, name, srn, newSrn); // Updated method call
        } else if (data.contains("search=")) {
            String term = URLDecoder.decode(data.split("=")[1], "UTF-8");
            handleGet(socket, term);
            return;
        } else {
            String[] parts = data.split("&");
            String name = URLDecoder.decode(parts[0].split("=")[1], "UTF-8");
            String srn = URLDecoder.decode(parts[1].split("=")[1], "UTF-8");
            addStudent(name, srn);
        }

        writeRedirect(socket, "/");
    }

    // Handle GET requests
    private static void handleGet(Socket socket) throws Exception {
        handleGet(socket, "");
    }

    private static void handleGet(Socket socket, String search) throws Exception {
        String html = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("web/index.html")));
        String table = buildTable(search);
        html = html.replace("{{table}}", table);
        
        // Ensure the button is placed correctly in the HTML
        html += "<div class='show-all-container'><form method='GET' action='/'><button type='submit'>Show All</button></form></div>";
        
        writeResponse(socket, html);
    }
    
    private static void handleCSS(Socket socket) throws Exception {
        try {
            String css = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("web/style.css")));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/css");
            out.println("Content-Length: " + css.length());
            out.println();
            out.println(css);
            out.flush();
        } catch (Exception e) {
            write404(socket);
        }
    }
    
    private static void handleJS(Socket socket) throws Exception {
        try {
            String js = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("web/script.js")));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/javascript");
            out.println("Content-Length: " + js.length());
            out.println();
            out.println(js);
            out.flush();
        } catch (Exception e) {
            write404(socket);
        }
    }
    
    private static void write404(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/html");
        out.println();
        out.println("<h1>404 Not Found</h1>");
        out.flush();
    }

    // 🧩 Step 2: Create connection
    private static Connection createConnection() throws Exception {
        String host = System.getenv("DB_HOST");
        String port = System.getenv("DB_PORT");
        String db = System.getenv("DB_NAME");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASSWORD");

        return DriverManager.getConnection(
            "jdbc:mysql://" + host + ":" + port + "/" + db + "?allowPublicKeyRetrieval=true&useSSL=false",
            user, pass);
    }

    // 🧩 Step 3 & 4: Create statement & Execute queries
    private static void addStudent(String name, String srn) throws Exception {
        try (Connection con = createConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO students(name, srn) VALUES (?, ?)");
            ps.setString(1, name);
            ps.setString(2, srn);
            ps.executeUpdate();
        }
        // 🧩 Step 5: Connection closed automatically (try-with-resources)
    }

    private static void deleteStudent(int id) throws Exception {
        try (Connection con = createConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM students WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static void updateStudent(int id, String name, String srn, String newSrn) throws Exception {
        try (Connection con = createConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE students SET name=?, srn=? WHERE id=?");
            ps.setString(1, name);
            ps.setString(2, newSrn); // Updated to use new SRN
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    // Build student table dynamically
    private static String buildTable(String search) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Connection con = createConnection()) {
            String query = "SELECT * FROM students";
            if (!search.isEmpty()) {
                query += " WHERE name LIKE ? OR srn LIKE ?";
            }
            PreparedStatement ps = con.prepareStatement(query);
            if (!search.isEmpty()) {
                ps.setString(1, "%" + search + "%");
                ps.setString(2, "%" + search + "%");
            }
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                sb.append("<tr>")
                  .append("<td>").append(rs.getInt("id")).append("</td>")
                  .append("<td>").append(rs.getString("name")).append("</td>")
                  .append("<td>").append(rs.getString("srn")).append("</td>")
                  .append("<td>")
                  // Delete button
                  .append("<form method='POST' style='display:inline'><input type='hidden' name='deleteId' value='")
                  .append(rs.getInt("id")).append("'><button class='delBtn'>Delete</button></form>")
                  // Update button
                  .append("<form method='POST' style='display:inline'>")
                  .append("<input type='hidden' name='updateId' value='").append(rs.getInt("id")).append("'>")
                  .append("<input name='name' value='").append(rs.getString("name")).append("'>")
                  .append("<input name='srn' value='").append(rs.getString("srn")).append("'>")
                  .append("<input name='newSrn' placeholder='New SRN'>") // New input for new SRN
                  .append("<button class='updBtn'>Update</button>")
                  .append("</form>")
                  .append("</td></tr>");
            }
        }
        return sb.toString();
    }

    private static void writeResponse(Socket socket, String body) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html");
        out.println("Content-Length: " + body.length());
        out.println();
        out.println(body);
        out.flush();
    }

    private static void writeRedirect(Socket socket, String location) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.println("HTTP/1.1 302 Found");
        out.println("Location: " + location);
        out.println();
        out.flush();
    }
}
