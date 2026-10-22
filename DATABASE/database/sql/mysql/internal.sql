
SELECT m.name, b.title, i.issue_date
FROM Member m
JOIN Issue i ON m.member_id = i.member_id
JOIN Book b ON i.book_id = b.book_id;


SELECT b.*
FROM Book b
LEFT JOIN Issue i ON b.book_id = i.book_id
WHERE i.book_id IS NULL;


SELECT m.name, COUNT(i.issue_id) AS total_books
FROM Member m
JOIN Issue i ON m.member_id = i.member_id
GROUP BY m.member_id, m.name
HAVING COUNT(i.issue_id) > 2;





INSERT INTO Issue
(issue_id, book_id, member_id, issue_date, return_date)
VALUES
(7, 5, 4, '2026-10-10', '2026-10-05');
