/**
 * This file extends libnetutils by some CCMNI (Mediatek-specific RIL-data driver) functions that the K4000 PRO depends on.
 * It's ugly, deal with it.
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>

#include <sys/socket.h>
#include <sys/select.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <netdb.h>

#include <linux/if.h>
#include <linux/if_ether.h>
#include <linux/if_arp.h>
#include <linux/netlink.h>
#include <linux/route.h>
#include <linux/ipv6_route.h>
#include <linux/rtnetlink.h>
#include <linux/sockios.h>
#include <linux/un.h>

#include "netutils/ifc.h"

#ifdef ANDROID
#define LOG_TAG "NetUtils"
#include <cutils/log.h>
#include <cutils/properties.h>
#else
#include <stdio.h>
#include <string.h>
#define ALOGD printf
#define ALOGW printf
#endif

#define SIOCSTXQSTATE (SIOCDEVPRIVATE + 0)  //start/stop ccmni tx queue
#define SIOCSCCMNICFG (SIOCDEVPRIVATE + 1)  //configure ccmni/md remapping

extern "C" void ifc_init_ifr(const char *name, struct ifreq *ifr)
{
    memset(ifr, 0, sizeof(struct ifreq));
    strncpy(ifr->ifr_name, name, IFNAMSIZ);
    ifr->ifr_name[IFNAMSIZ - 1] = 0;
}

extern "C" int ifc_netd_sock_init(void)
{
    int ifc_netd_sock;
    const int one = 1;
    struct sockaddr_un netd_addr;
  
        ifc_netd_sock = socket(AF_UNIX, SOCK_STREAM, 0);
        if (ifc_netd_sock < 0) {
            ALOGD("ifc_netd_sock_init: create socket failed");
            return -1;
        }
  
        setsockopt(ifc_netd_sock, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));
        memset(&netd_addr, 0, sizeof(netd_addr));
        netd_addr.sun_family = AF_UNIX;
        strlcpy(netd_addr.sun_path, "/dev/socket/netd",
            sizeof(netd_addr.sun_path));
        if (TEMP_FAILURE_RETRY(connect(ifc_netd_sock,
                     (const struct sockaddr*) &netd_addr,
                     sizeof(netd_addr))) != 0) {
            ALOGD("ifc_netd_sock_init: connect to netd failed, fd=%d, err: %d(%s)", 
                ifc_netd_sock, errno, strerror(errno));
            close(ifc_netd_sock);
            return -1;
        }
  
    ALOGD("ifc_netd_sock_init fd=%d", ifc_netd_sock);
    return ifc_netd_sock;
}

/*do not call this function in netd*/
extern "C" int ifc_set_throttle(const char *ifname, int rxKbps, int txKbps)
{
    FILE* fnetd = NULL;
    int ret = -1;
    int seq = 1;
    char rcv_buf[24];
	int nread = 0;
	int netd_sock = 0;
	
    ALOGD("enter ifc_set_throttle: ifname = %s, rx = %d kbs, tx = %d kbs", ifname, rxKbps, txKbps);

    netd_sock = ifc_netd_sock_init();
    if(netd_sock <= 0)
        goto exit;
    
    // Send the request.
    fnetd = fdopen(netd_sock, "r+");
	if(fnetd == NULL){
		ALOGE("open netd socket failed, err:%d(%s)", errno, strerror(errno));
		goto exit;
	}
    if (fprintf(fnetd, "%d interface setthrottle %s %d %d", seq, ifname, rxKbps, txKbps) < 0) {
        goto exit;
    }
    // literal NULL byte at end, required by FrameworkListener
    if (fputc(0, fnetd) == EOF ||
        fflush(fnetd) != 0) {
        goto exit;
    }
    ret = 0;

	//Todo: read the whole response from netd
	nread = fread(rcv_buf, 1, 20, fnetd);
	rcv_buf[23] = 0;
	ALOGD("response: %s", rcv_buf);
exit:
    if (fnetd != NULL) {
        fclose(fnetd);
    }
  
    return ret;
}

/*do not call this function in netd*/
extern "C" int ifc_set_fwmark_rule(const char *ifname, int mark, int add)
{
    FILE* fnetd = NULL;
    int ret = -1;
    int seq = 2;
    char rcv_buf[24];
	  int nread = 0;
	  const char* op;
    int netd_sock = 0;
    
    if (add) {
        op = "add";
    } else {
        op = "remove";
    }
    ALOGD("enter ifc_set_fwmark_rule: ifname = %s, mark = %d, op = %s", ifname, mark, op);

    netd_sock = ifc_netd_sock_init();
    if(netd_sock <= 0)
        goto exit;
    
    // Send the request.
    fnetd = fdopen(netd_sock, "r+");
	if(fnetd == NULL){
		ALOGE("open netd socket failed, err:%d(%s)", errno, strerror(errno));
		goto exit;
	}
    if (fprintf(fnetd, "%d network fwmark %s %s %d", seq, op, ifname, mark) < 0) {
        goto exit;
    }
    // literal NULL byte at end, required by FrameworkListener
    if (fputc(0, fnetd) == EOF ||
        fflush(fnetd) != 0) {
        goto exit;
    }
    ret = 0;

	//Todo: read the whole response from netd
	nread = fread(rcv_buf, 1, 20, fnetd);
	rcv_buf[23] = 0;
	ALOGD("ifc_set_fwmark_rule response: %s", rcv_buf);
exit:
    if (fnetd != NULL) {
        fclose(fnetd);
    }
  
    return ret;
}

extern "C" int ifc_set_txq_state(const char *ifname, int state)
{
    struct ifreq ifr;
    int ret, ctl_sock;
    
    memset(&ifr, 0, sizeof(struct ifreq));
    strncpy(ifr.ifr_name, ifname, IFNAMSIZ);
    ifr.ifr_name[IFNAMSIZ - 1] = 0;
    ifr.ifr_ifru.ifru_ivalue = state;
    
    ctl_sock = socket(AF_INET, SOCK_DGRAM, 0);
    if(ctl_sock < 0){
    	ALOGE("create ctl socket failed\n");
    	return -1;
    }
    ret = ioctl(ctl_sock, SIOCSTXQSTATE, &ifr);
    if(ret < 0)
    	ALOGE("ifc_set_txq_state failed, err:%d(%s)\n", errno, strerror(errno));
    else
    	ALOGI("ifc_set_txq_state as %d, ret: %d\n", state, ret);
    
    close(ctl_sock);
    
    return ret;
}

extern "C" int ifc_ccmni_md_cfg(const char *ifname, int md_id)
{
    struct ifreq ifr;
    int ret = 0;
    int ctl_sock = 0;
    
    ifc_init_ifr(ifname, &ifr);
    ifr.ifr_ifru.ifru_ivalue = md_id;

    ctl_sock = socket(AF_INET, SOCK_DGRAM, 0);
    if(ctl_sock < 0){
    	ALOGD("ifc_ccmni_md_cfg: create ctl socket failed\n");
    	return -1;
    }
		
    if(ioctl(ctl_sock, SIOCSCCMNICFG, &ifr) < 0) {
    	ALOGD("ifc_ccmni_md_configure(ifname=%s, md_id=%d) error:%d(%s)", \
        	ifname, md_id, errno, strerror(errno));
    	ret = -1;
    } else {
    	ALOGD("ifc_ccmni_md_configure(ifname=%s, md_id=%d) OK", ifname, md_id);
    }
    
    close(ctl_sock);
    return ret;
}

