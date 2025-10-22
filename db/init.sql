CREATE TABLE IF NOT EXISTS students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    srn VARCHAR(20)
);

INSERT INTO students(name, srn) VALUES ('Alice', 'SRN001'), ('Bob', 'SRN002');
