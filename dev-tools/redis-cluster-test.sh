redis-server /usr/local/etc/redis2.conf &
redis-server /usr/local/etc/redis.conf &
redis-sentinel /usr/local/etc/redis-sentinel.conf & 
redis-sentinel /usr/local/etc/redis-sentinel2.conf & 
#redis-cli -h 127.0.0.1 -p 6379 -a foobared
#redis-cli -h 127.0.0.1 -p 6378 -a foobared
