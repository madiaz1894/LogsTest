
# (1) Write MySQL query to find IPs that mode more than a certain number of requests for a given time period. 

SELECT ip
FROM log_records
WHERE LOGDATE BETWEEN UNIX_TIMESTAMP(A) AND UNIX_TIMESTAMP(B)
GROUP BY ip
HAVING COUNT(*)> C;

# A: startDate in yyyy-MM-dd.HH:mm:ss format
# B: endDate in yyyy-MM-dd.HH:mm:ss format
# C: threshold


# (2) Write MySQL query to find requests made by a given IP

SELECT COUNT(*) count_requests
FROM log_records
WHERE IP LIKE '192.168.102.136'
GROUP BY (IP)
