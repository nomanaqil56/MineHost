<?php
$sock = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
socket_set_option($sock, SOL_SOCKET, SO_RCVTIMEO, ['sec' => 2, 'usec' => 0]);
$magic = "\x00\xff\xff\x00\xfe\xfe\xfe\xfe\xfd\xfd\xfd\xfd\x12\x34\x56\x78";

// 64-bit Big-Endian timestamp
$time = (int)(microtime(true) * 1000);
$timeHigh = ($time >> 32) & 0xffffffff;
$timeLow = $time & 0xffffffff;

// 64-bit Big-Endian client GUID
$guidHigh = 0x12345678;
$guidLow = 0x9abcdef0;

$ping = "\x01" . pack("NN", $timeHigh, $timeLow) . $magic . pack("NN", $guidHigh, $guidLow);
socket_sendto($sock, $ping, strlen($ping), 0, "127.0.0.1", 19132);

$buf = "";
$from = "";
$port = 0;
$len = @socket_recvfrom($sock, $buf, 2048, 0, $from, $port);
if ($len > 35 && ord($buf[0]) === 0x1c) {
    $motdLen = unpack("n", substr($buf, 33, 2))[1];
    $motd = substr($buf, 35, $motdLen);
    echo "PONG_OK:" . $motd . "\n";
} else {
    echo "PONG_FAIL: len=$len\n";
}
socket_close($sock);

