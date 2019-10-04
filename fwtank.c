#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <stdlib.h>
#include <string.h>

#define LEV 5 //merkle tree levels
#define MAX 20
#define HSIZE 20   //size of hash in bytes
#define HINSIZE 64 //size of input to hash function
#define CSIZE 24
#define ASIZE 60		 //number of addressable 8-byte locations
#define N (1 << LEV) / 6 //number of systems each with six sensors
typedef unsigned char byte;
typedef long long l8;
struct module
{
	byte LAMBDA[HSIZE];
	byte GAMMA[2][HSIZE];
	byte Cctr;
	byte sroot[HSIZE];
	byte droot[HSIZE];
	byte root[HSIZE];
	byte lambda[4][HSIZE];
	byte kappa[HSIZE];
	l8 peer;
	l8 T;
	l8 V[60];
	byte c[24];
	byte ltmp1[64];
	byte ltmp2[64];
	byte htmp1[HSIZE];
	byte htmp2[HSIZE];
};
struct module M;
struct leaf
{
	l8 Sid;
	l8 Dval;
	l8 S1;
	l8 S2;
	l8 S3;
	l8 Sval;
	l8 Texp;
	l8 Tmin;
	l8 EV1;
	l8 EV2;
	l8 EV3;
	byte c[CSIZE];
};
struct bt
{
	unsigned int key;
	byte data[HSIZE];
	struct bt *left;
	struct bt *right;
};
struct inst
{
	byte h[HSIZE];
	byte order;
};
struct dleaf
{ //leaf of deployment tree
	l8 Sid;
	l8 Mid;
	l8 Rtt;
};
typedef struct leaf leaf;
typedef struct dleaf dleaf;
struct wtank
{
	byte height;
	byte Pump;
	byte Valve;
};
typedef struct wtank wt;
typedef struct inst inst;
typedef struct bt node;
void h1(byte *input, byte *dig)
{
	byte i, k, a, j, l, s[256], ilen = 64;
	for (i = 0; i < 255; i++)
		s[i] = i;
	s[255] = 255;
	for (l = 0; l < 8; l++)
	{
		j = k = 0;
		for (i = 0; i < 255; i++)
		{
			if (k > ilen - 1)
				k = 0;
			a = s[i];
			j = j ^ s[i] ^ input[k];
			s[i] = s[j];
			s[j] = a;
			k++;
		}
		if (k > ilen - 1)
			k = 0;
		a = s[255];
		j = j ^ s[255] ^ input[k];
		s[255] = s[j];
		s[j] = a;
	}
	for (i = 0; i < 20; i++)
		dig[i] = s[i];
}
void hnode(byte c[20], byte e[20], byte order, byte *out)
{
	byte in[64] = {0};
	if (order == 0)
	{
		memcpy(in, c, 20);
		memcpy(in + 20, e, 20);
	}
	else
	{
		memcpy(in, e, 20);
		memcpy(in + 20, c, 20);
	}
	h1(in, out);
	return;
}

void hleaf(void *x, void *y, void *z, byte *out)
{
	//x is the beginning of five immutable fields - 40 bytes
	//y is the beginning of immutable code - 24 bytes
	//z is the beginning of mutable fields - 48 bytes
	byte in[64] = {0};
	byte tmp1[20], tmp2[20];
	memcpy(in, x, 40);
	memcpy(in + 40, y, 24);
	h1(in, tmp1);
	memset(in, 0, 64);
	memcpy(in, z, 48);
	h1(in, tmp2);
	memcpy(in, tmp1, 20);
	memcpy(in + 20, tmp2, 20);
	memset(in + 40, 0, 24);
	h1(in, out);
}

void fhleaf(void *x, void *y, void *z, byte *out)
{
	memset(M.ltmp1, 0, 64);
	memset(M.ltmp2, 0, 64);
	memcpy(M.ltmp1, x, 40);
	memcpy(M.ltmp1 + 40, y, 24);
	h1(M.ltmp1, M.ltmp2);
	memset(M.ltmp1, 0, 64);
	memcpy(M.ltmp1, z, 48);
	h1(M.ltmp1, M.ltmp2 + 20);
	h1(M.ltmp2, out);
}
void l2r(byte ln[20], inst *iv, int l, byte *out)
{
	int i;
	byte *in = ln;
	byte *ot = out;
	for (i = 0; i < l; i++)
	{
		hnode(in, iv[i].h, iv[i].order, out);
		in = out;
	}
	return;
}
void fl2r(byte ln[20], byte *lhashes, byte l, int index, byte *out)
{
	int i;
	byte *in = ln;
	byte *ot = out;
	byte order;
	for (i = 0; i < l; i++)
	{
		order = (byte)index % 2;
		printf("order %d  ", order);
		hnode(in, &lhashes[i * 20], order, out);
		in = out;
		index = index >> 1;
	}
	return;
}
byte meop(byte op, byte ad1, byte ad2, l8 *V)
{
	byte aop, out;
	l8 lout, in1, in2;
	if (op == 0)
		return 0; //stop execution
	aop = op >> 2;
	out = op - (aop << 2);
	if (aop == 1)
		lout = V[ad1] + V[ad2];
	else if (aop == 2)
		lout = V[ad1] - V[ad2];
	else if (aop == 3)
		lout = V[ad2] - V[ad1];
	else if (aop == 4)
		lout = V[ad1];
	else if (aop == 5)
		lout = !V[ad1];
	else if (aop == 6)
		lout = ~V[ad1];

	else if (aop == 7)
		lout = V[ad1] & V[ad2];
	else if (aop == 8)
		lout = V[ad1] & (~V[ad2]);
	else if (aop == 9)
		lout = (~V[ad1]) & V[ad2];
	else if (aop == 10)
		lout = (~V[ad1]) & (~V[ad2]);

	else if (aop == 11)
		lout = V[ad1] | V[ad2];
	else if (aop == 12)
		lout = V[ad1] | (~V[ad2]);
	else if (aop == 13)
		lout = (~V[ad1]) | V[ad2];
	else if (aop == 14)
		lout = (~V[ad1]) | (~V[ad2]);

	else if (aop == 15)
		lout = V[ad1] && V[ad2];
	else if (aop == 16)
		lout = V[ad1] && (!V[ad2]);
	else if (aop == 17)
		lout = (!V[ad1]) && V[ad2];
	else if (aop == 18)
		lout = (!V[ad1]) && (!V[ad2]);

	else if (aop == 19)
		lout = V[ad1] || V[ad2];
	else if (aop == 20)
		lout = V[ad1] || (!V[ad2]);
	else if (aop == 21)
		lout = (!V[ad1]) || V[ad2];
	else if (aop == 22)
		lout = (!V[ad1]) || (!V[ad2]);

	if (out == 0)
		V[0] = lout;
	else if (out == 1)
		V[24] = lout;
	else if (out == 2)
		V[25] = lout;
	else if (out == 3)
		V[26] = lout;
	return 1;
}

byte mexec(byte code[24], l8 *V)
{
	byte ret = 1;
	byte ct = 0;
	while (ret && (ct < 8))
	{
		ret = meop(code[ct * 3 + 0], code[ct * 3 + 1], code[ct * 3 + 2], V);
		ct++;
	}
	return ret;
}
void Fc1(byte nu[20], inst *iv, byte l, byte *rt)
{
	memset(M.ltmp1, 0, 64);
	M.ltmp1[0] = 1;
	memcpy(&(M.ltmp1[1]), nu, 20);
	l2r(nu, iv, l, &(M.ltmp1[21]));
	memcpy(&(M.ltmp1[41]), M.LAMBDA, 20);
	h1(M.ltmp1, rt);
}

void Fc2(byte nu[20], byte nun[20], inst *iv, byte l, byte *rt)
{
	memset(M.ltmp1, 0, 64);
	memset(M.ltmp2, 0, 64);
	M.ltmp1[0] = 2;
	memcpy(M.ltmp2, nu, 20);
	l2r(nu, iv, l, &(M.ltmp2[20]));
	h1(M.ltmp2, &(M.ltmp1[1]));
	memcpy(M.ltmp2, nun, 20);
	l2r(nun, iv, l, &(M.ltmp2[20]));
	h1(M.ltmp2, &(M.ltmp1[21]));
	memcpy(&(M.ltmp1[41]), M.LAMBDA, 20);
	h1(M.ltmp1, rt);
}
void Finit(byte sroot[20], byte droot[20])
{
	memcpy(M.sroot, sroot, 20);
	memcpy(M.root, sroot, 20);
	memcpy(M.droot, droot, 20);
	memset(M.LAMBDA, 0, 20);
}
void Flrr(l8 Sid, l8 Sval, l8 Tr)
{
	M.V[12] = Sid;
	M.V[13] = Sval;
	M.V[14] = Tr;
}
byte Flsr(byte pos, byte nu[20], byte mu[20])
{
	memset(M.ltmp1, 0, 64);
	M.ltmp1[0] = 1;
	memcpy(&(M.ltmp1[1]), nu, 20);
	memcpy(&(M.ltmp1[21]), M.root, 20);
	memcpy(&(M.ltmp1[41]), M.LAMBDA, 20);
	h1(M.ltmp1, M.htmp1);
	if (memcmp(M.htmp1, mu, 20))
	{
		printf("problem in Flsr\n", pos);
		return 1;
	}
	memcpy(&M.lambda[pos], nu, 20);
	return 0;
}
void Fllf(byte pos, void *l1, void *l2, void *l3)
{
	fhleaf(l1, l2, l3, M.htmp1);
	if (memcmp(M.htmp1, &(M.lambda[pos]), 20))
	{
		printf("Fllf %d \n", pos);
		return;
	}
	memcpy(&(M.V[16 + pos * 11]), l1, 40);
	memcpy(&(M.V[21 + pos * 11]), l3, 48);
	if (pos == 0)
		memcpy(M.c, l2, 24);
	return;
}
void Fupd(byte nrt[20], byte mu[20])
{
	if (M.V[16] != M.V[12])
	{
		printf("two SIds \n");
		return;
	}
	if (M.V[18] != M.V[27])
		return;
	if (M.V[19] != M.V[38])
		return;
	if (M.V[20] != M.V[49])
		return;
	if ((M.V[14] + M.V[17]) < M.V[22])
	{
		printf("time \n");
		return;
	}
	M.V[21] = M.V[13];
	M.V[22] = M.V[14] + M.V[17];
	M.V[23] = M.V[22];
	M.V[23] = ((M.V[34] > 0) && (M.V[34] < M.V[23])) ? M.V[34] : M.V[23];
	M.V[23] = ((M.V[45] > 0) && (M.V[45] < M.V[23])) ? M.V[45] : M.V[23];
	M.V[23] = ((M.V[56] > 0) && (M.V[56] < M.V[23])) ? M.V[56] : M.V[23];
	mexec(M.c, M.V);
	fhleaf(&(M.V[16]), &M.c, &M.V[21], M.htmp1); //M.htmp1 is the new leaf hash
	memset(M.ltmp1, 0, 64);
	memset(M.ltmp2, 0, 64);
	memcpy(M.ltmp1, M.lambda, 20);
	memcpy(M.ltmp1 + 20, M.root, 20);
	h1(M.ltmp1, &M.ltmp2[1]);
	M.ltmp2[0] = 2;
	memset(M.ltmp1, 0, 64);
	memcpy(M.ltmp1, M.htmp1, 20);
	memcpy(&M.ltmp1[20], nrt, 20);
	h1(M.ltmp1, &M.ltmp2[21]);
	memcpy(&M.ltmp2[41], M.LAMBDA, 20);
	h1(M.ltmp2, M.htmp2);
	if (memcmp(M.htmp2, mu, 20))
	{
		printf("type 2 cert fail \n");
		return;
	}
	memcpy(M.root, nrt, 20);
	memset(M.lambda, 0, 80);
	memset(&M.V[12], 0, 384); //48 l8 12-15, and 4 times 11
	memset(M.c, 0, 24);
}

node *put(unsigned int key, node *root, unsigned char *dp, int get)
{
	node *cur;
	if (root == NULL)
	{
		root = (node *)malloc(sizeof(node));
		//printf("inserting root %d\n", key);
		root->key = key;
		memcpy(root->data, dp, 20);
		root->left = NULL;
		root->right = NULL;
		return root;
	}
	cur = root;
	while (cur != NULL)
	{
		if (key < cur->key)
		{
			if (cur->left != NULL)
				cur = cur->left;
			else
			{
				if (get)
				{
					//printf("%d not found\n",key);
					return NULL;
				}
				cur->left = (node *)malloc(sizeof(node));
				cur = cur->left;
				//printf("inserting left %d \n", key);
				cur->key = key;
				memcpy(cur->data, dp, 20);
				cur->left = NULL;
				cur->right = NULL;
				return cur;
			}
		}
		else if (key > cur->key)
		{
			if (cur->right != NULL)
				cur = cur->right;
			else
			{
				if (get)
				{
					printf("%d not found \n", key);
					return NULL;
				}
				cur->right = (node *)malloc(sizeof(node));
				cur = cur->right;
				//printf("inserting right  %d \n", key);
				cur->key = key;
				memcpy(cur->data, dp, 20);
				cur->left = NULL;
				cur->right = NULL;
				return cur;
			}
		}
		else
		{ //key == cur->key
			if (get)
			{
				memcpy(dp, cur->data, 20); //printf("getting %d \n",key);
			}
			else
			{
				memcpy(cur->data, dp, 20); //printf("updating %d \n",key);
			}
			return cur;
		}
	}
}

void btfree(node *x)
{
	if (x != NULL)
	{
		//printf("freeing %d \n",x->key );
		btfree(x->left);
		btfree(x->right);
		free(x);
		x = NULL;
	}
	return;
}

unsigned long getkey(unsigned long i, byte j)
{
	//i is x-coordinate and j is y coordinate
	return ((1 << 30) + (i << 6) + j);
}

node *inittree(int lev, node *root)
{
	//build a binary tree for a merkle tree with merkle tree root as
	//the root node for the binary tree. lev is number of levels
	int i, n, j;
	node *ret;
	byte tmp[20] = {0};
	if (root != NULL)
		return root;
	for (i = 0; i <= lev; i++)
	{
		n = 1 << i;
		for (j = 0; j < n; j++)
		{
			ret = put(getkey(j, lev - i), root, tmp, 0);
			if (root == NULL)
				root = ret;
		}
	}
	return root;
}

void mtreerow0(leaf *lf, int lev, node *btroot)
{
	int i, n;
	n = 1 << lev;
	byte tmp[20];
	node *nret;
	for (i = 0; i < n; i++)
	{
		hleaf((void *)&lf[i].Sid, (void *)&lf[i].c, (void *)&lf[i].Sval, tmp);
		nret = put(getkey(i, 0), btroot, tmp, 0);
	}
}
void buildmerkle(int lev, node *btroot)
{
	int i, j, n, l, r;
	n = 1 << lev;
	j = 0;
	byte tl[20];
	byte tr[20];
	byte tmp[20];
	while (n = n >> 1)
	{
		j = j++; //generating level 1 nodes
		for (i = 0; i < n; i++)
		{
			l = getkey(2 * i, j - 1);
			r = getkey(2 * i + 1, j - 1);
			put(l, btroot, tl, 1);
			put(r, btroot, tr, 1);
			hnode(tl, tr, 0, tmp);
			put(getkey(i, j), btroot, tmp, 0);
		}
	}
}
void getvector(int index, inst *iv, int l, node *root)
{
	int i, key, cs;
	byte order;
	for (i = 0; i < l; i++)
	{
		order = (byte)index % 2;
		cs = (order == 1) ? index - 1 : index + 1;
		key = getkey(cs, i);
		put(key, root, &(iv[i].h[0]), 1);
		iv[i].order = order;
		index = index >> 1;
	}
	return;
}

void updateleafnode(int index, byte *in, inst *iv, int l, node *root)
{
	int i;
	byte *inp = in;
	byte tmp[20];
	int key;
	for (i = 0; i < l; i++)
	{
		hnode(inp, iv[i].h, iv[i].order, tmp);
		index = index >> 1;
		put(getkey(index, i + 1), root, tmp, 0); //update
		inp = tmp;
	}
}

void DesignWT(leaf *lf)
{
	int i; //number of systems;
	byte codepp[24] = {88, 21, 43, 61, 0, 54, 72, 32, 43, 77, 0, 24};
	byte codehh[24] = {72, 21, 32, 65, 0, 43, 68, 21, 43, 60, 0, 54, 77, 0, 24, 60, 32, 43, 60, 0, 54, 77, 0, 24};
	byte codeval0[24] = {61, 35, 46, 18, 24, 0};
	byte codeval[24] = {61, 35, 46, 62, 24, 58}; //58 is third pos EV2
	//for (i=0;i<24; i++) printf("%3d - %3d - %3d",codepp[i],codeval[i],codehh[i]);
	for (i = 0; i < N; i++)
	{
		lf[i * 6].Sid = i * 6 + 1; //pump
		lf[i * 6].Dval = 4;
		lf[i * 6].S1 = i * 6 + 2; //valve
		lf[i * 6].S2 = i * 6 + 3; //hh
		lf[i * 6].S3 = i * 6 + 6; //ll
		memset((void *)&(lf[i * 6].Sval), 0, 48);
		memcpy((void *)&(lf[i * 6].c[0]), (void *)codepp, 24);

		lf[i * 6 + 1].Sid = i * 6 + 2; //valve
		lf[i * 6 + 1].Dval = 4;
		lf[i * 6 + 1].S1 = i * 6 + 1; //pump
		lf[i * 6 + 1].S2 = i * 6 + 3; //hh
		memset((void *)&(lf[i * 6 + 1].Sval), 0, 48);
		if (i == 0)
		{
			lf[i * 6 + 1].S3 = 0; //unused for i=0
			memcpy(lf[i * 6 + 1].c, codeval0, 24);
		}
		else
		{
			lf[i * 6 + 1].S3 = (i - 1) * 6 + 2; //previous system's valve
			memcpy(lf[i * 6 + 1].c, codeval, 24);
		}
		lf[i * 6 + 2].Sid = i * 6 + 3; //hh
		lf[i * 6 + 2].Dval = 4;
		lf[i * 6 + 2].S1 = i * 6 + 4; //hi
		lf[i * 6 + 2].S2 = i * 6 + 5; //lo
		lf[i * 6 + 2].S3 = i * 6 + 6; //ll
		memset((void *)&(lf[i * 6 + 2].Sval), 0, 48);
		memcpy(lf[i * 6 + 2].c, codehh, 24);

		lf[i * 6 + 3].Sid = i * 6 + 4; //hi
		lf[i * 6 + 3].Dval = 4;
		lf[i * 6 + 3].S1 = 0;
		lf[i * 6 + 3].S2 = 0;
		lf[i * 6 + 3].S3 = 0;
		memset((void *)&(lf[i * 6 + 3].Sval), 0, 72);

		lf[i * 6 + 4].Sid = i * 6 + 5; //lo
		lf[i * 6 + 4].Dval = 4;
		lf[i * 6 + 4].S1 = 0;
		lf[i * 6 + 4].S2 = 0;
		lf[i * 6 + 4].S3 = 0;
		memset((void *)&(lf[i * 6 + 4].Sval), 0, 72);

		lf[i * 6 + 5].Sid = i * 6 + 6; //lo
		lf[i * 6 + 5].Dval = 4;
		lf[i * 6 + 5].S1 = 0;
		lf[i * 6 + 5].S2 = 0;
		lf[i * 6 + 5].S3 = 0;
		memset((void *)&(lf[i * 6 + 5].Sval), 0, 72);
	}
}
void updateleaf(leaf *plf, int index, node *root, l8 Val, l8 Tr, l8 *V)
{
	l8 Tmin, tmp;
	inst iv[LEV];
	inst ivs[LEV];
	byte pold[HSIZE];
	byte pnew[HSIZE];
	byte p1[HSIZE];
	byte nrl2r[HSIZE];
	byte croot[HSIZE];
	byte nroot[HSIZE];
	byte t20[HSIZE];
	int i;
	Flrr(plf[index].Sid, Val, Tr);
	put(getkey(0, LEV), root, croot, 1); //get current root
	hleaf((void *)&plf[index].Sid, (void *)&plf[index].c, (void *)&plf[index].Sval, pold);
	getvector(index, iv, LEV, root);
	// generate type1 cert , check root in cert, load in pos0
	l2r(pold, iv, LEV, t20);
	if (memcmp(t20, croot, 20) != 0)
		printf("WE HAVE PROBLEMS! (POS0) \n");
	if (memcmp(t20, M.root, 20) != 0)
		printf("WE HAVE PROBLEMS! (POS0) mod \n");
	for (i = 16; i < 60; i++)
		V[i] = 0;
	Fc1(pold, iv, LEV, t20); //t20 is the MAC
	Flsr(0, pold, t20);
	Fllf(0, (void *)&plf[index].Sid, (void *)&plf[index].c, (void *)&plf[index].Sval);
	plf[index].Sval = Val;
	plf[index].Texp = plf[index].Dval + Tr;
	tmp = plf[index].S1;
	if (tmp > 0)
	{
		memcpy((void *)&V[27], (void *)&plf[tmp - 1].Sid, 88);
		hleaf((void *)&plf[tmp - 1].Sid, (void *)&plf[tmp - 1].c, (void *)&plf[tmp - 1].Sval, p1);
		getvector(tmp - 1, ivs, LEV, root);
		//generte typ1 cert, load on to pos1
		l2r(p1, ivs, LEV, t20);
		if (memcmp(t20, croot, 20) != 0)
			printf("WE HAVE PROBLEMS! (POS1) \n");
		Fc1(p1, ivs, LEV, t20); //t20 is the MAC
		Flsr(1, p1, t20);
		Fllf(1, &plf[tmp - 1].Sid, &plf[tmp - 1].c, &plf[tmp - 1].Sval);
	}
	tmp = plf[index].S2;
	if (tmp > 0)
	{
		memcpy((void *)&V[38], (void *)&plf[tmp - 1].Sid, 88);
		hleaf((void *)&plf[tmp - 1].Sid, (void *)&plf[tmp - 1].c, (void *)&plf[tmp - 1].Sval, p1);
		getvector(tmp - 1, ivs, LEV, root);
		l2r(p1, ivs, LEV, t20);
		if (memcmp(t20, croot, 20) != 0)
			printf("WE HAVE PROBLEMS! (POS2) \n");
		Fc1(p1, ivs, LEV, t20); //t20 is the MAC
		Flsr(2, p1, t20);
		Fllf(2, &plf[tmp - 1].Sid, &plf[tmp - 1].c, &plf[tmp - 1].Sval);
	}
	tmp = plf[index].S3;
	if (tmp > 0)
	{
		memcpy((void *)&V[49], (void *)&plf[tmp - 1].Sid, 88);
		hleaf((void *)&plf[tmp - 1].Sid, (void *)&plf[tmp - 1].c, (void *)&plf[tmp - 1].Sval, p1);
		getvector(tmp - 1, ivs, LEV, root);
		l2r(p1, ivs, LEV, t20);
		if (memcmp(t20, croot, 20) != 0)
			printf("WE HAVE PROBLEMS! (POS3) \n");
		Fc1(p1, ivs, LEV, t20); //t20 is the MAC
		Flsr(3, p1, t20);
		Fllf(3, &plf[tmp - 1].Sid, &plf[tmp - 1].c, &plf[tmp - 1].Sval);
	}
	if (plf[index].Sid == 0)
		return;
	memcpy((void *)&V[16], (void *)&plf[index].Sid, 88);
	V[23] = V[22];
	V[23] = ((V[34] > 0) && (V[34] < V[23])) ? V[34] : V[23];
	V[23] = ((V[45] > 0) && (V[45] < V[23])) ? V[45] : V[23];
	V[23] = ((V[56] > 0) && (V[56] < V[23])) ? V[56] : V[23];
	mexec(plf[index].c, V);
	plf[index].EV1 = V[24];
	plf[index].EV2 = V[25];
	plf[index].EV3 = V[26];
	plf[index].Tmin = V[23];
	hleaf((void *)&plf[index].Sid, (void *)&plf[index].c, (void *)&plf[index].Sval, pnew);
	l2r(pnew, iv, LEV, t20);			  //t20 is new root
	put(getkey(index, 0), root, pnew, 0); //get current root
	updateleafnode(index, pnew, iv, LEV, root);
	put(getkey(0, LEV), root, croot, 1); //get current root
	if (memcmp(t20, croot, 20) != 0)
		printf("WE HAVE PROBLEMS! (after update) \n");
	Fc2(pold, pnew, iv, LEV, t20);
	Fupd(croot, t20);
}

int main(int argc, char *argv[])
{

	//my declarations
	FILE *fp, *fpout;
	unsigned char buf[80] = {0};
	int chkret = 0;
	unsigned char *token;
	l8 input[32];
	l8 var, time; //time, last value to KILL

	//decl
	byte tmp[20] = {0};
	byte tmplong[20 * LEV] = {0};
	byte tmp1[20] = {0};
	byte tmp64[64] = {0};
	byte droot[20] = {0};
	byte sroot[20] = {0};
	l8 V[60];

	int i, j, k, key, n, pa, cs;
	leaf lf[1 << LEV];   //to be initialized with design values;
	dleaf dlf[1 << LEV]; //to be initialized with design values;
	inst iv[LEV];
	node *mtroot = NULL; //root of bt for main tree`
	node *dtroot = NULL; //root of bt for deploment tree
	node *ret = NULL;	//temporary node to collect return value
	node *simroot = NULL;

	mtroot = inittree(LEV, mtroot); //initialize main tree
	//at the end of this all nodes have been allocated
	//and filled with non sensical values;
	DesignWT(lf);
	mtreerow0(lf, LEV, mtroot);		//build row 0 of main tree
	buildmerkle(LEV, mtroot);		//construct main merkle tree
	dtroot = inittree(LEV, dtroot); //initialize dep tree
	for (i = 0; i < LEV; i++)
	{
		dlf[i].Sid = i + 1;
		dlf[i].Mid = 1;
		dlf[i].Rtt = 100;
		memcpy(tmp64, (void *)&dlf[i], 24);
		memset(tmp64 + 24, 0, 40);
		h1(tmp64, tmp);
		put(getkey(i, 0), dtroot, tmp, 0);
	}
	buildmerkle(LEV, dtroot); //construct main merkle tree
	put(getkey(0, LEV), mtroot, sroot, 1);
	put(getkey(0, LEV), dtroot, droot, 1);
	Finit(sroot, droot);
	if (memcmp(M.sroot, sroot, 20))
	{
		printf("fix this");
		return;
	}

	/*	
   	getvector(5, iv, LEV, mtroot);
		put(getkey(5,0), mtroot, tmp, 1);
		l2r(tmp,iv,LEV,tmp1);
		for (i=0; i< 20; i++) printf("%3d ", tmp1[i]); printf(" l2r \n");
		for (i=0;i<LEV;i++) memcpy(tmplong+i*20, &iv[i].h[0], 20);
		printf ("\n\n\n\n\n\n\n");

		fl2r(tmp, tmplong, LEV, 5, tmp1);
	   for (i=0; i< 20; i++) printf("%3d ", tmp1[i]); printf(" fl2r \n");
*/

	if (argc != 3)
	{
		fprintf(stderr, "Usage: fifoServer MYFIFO OUTFIFO\n");
		exit(1);
	}

	/*---------------------------------------------------------------------------------------
         Create the FIFO using mkfifo system call
         syntax: int mkfifo(const char *pathname, mode_t mode);
        ----------------------------------------------------------------------------------------*/

	ret = mkfifo(argv[1], S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH);
	ret = mkfifo(argv[2], S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH);
	/*----------------------------------------------------------------------------------------
        Here pathname corresponds to FIFO Name i.e MYFIFO, mode corresponds to permission. Since
        mkfifo can only be used to create a fifo file, no need to specify the file type as S_IFIFO.
        -------------------------------------------------------------------------------------------*/

	if (ret < 0)
	{
		perror("mkfifo failed");
		exit(errno);
	}

	/*--------------------------------------------------------
        Alternate way:
        umask(0);
        ret = mkfifo(FIFO_FILE, 0755)
        ---------------------------------------------------------*/

	while (1)
	{
		fp = fopen(argv[1], "r");
		fgets(buf, 80, fp);

		token = strtok(buf, " |,.-");
		i = 0;
		while (token != NULL)
		{
			sscanf(token, "%lld", &var);
			input[i++] = var;
			token = strtok(NULL, " |,.-");
		}

		printf("\nReceived inputs from GUI: ");
		for (i = 0; i < 32; i++)
		{
			printf("%lld ", input[i]);
		}

		time = input[0];

		for (i = 0; i < 5; i++)
		{

			updateleaf(lf, i * 6 + 5, mtroot, input[6], time, V); //ll
			updateleaf(lf, i * 6 + 4, mtroot, input[5], time, V); //lo
			updateleaf(lf, i * 6 + 3, mtroot, input[4], time, V); //hi
			updateleaf(lf, i * 6 + 2, mtroot, input[3], time, V); //hh
			updateleaf(lf, i * 6 + 0, mtroot, input[1], time, V); //pp
			updateleaf(lf, i * 6 + 1, mtroot, input[2], time, V); //val
			printf("\n %d  %d ", i + 1, lf[i * 6 + 1].EV2 > 0);

			printf(" time %lld \n ", time);
		}

		fflush(fp);

		fclose(fp);

		if ((fpout = fopen(argv[2], "w")) == NULL)
		{
			perror("fopen failed");
			exit(errno);
		}

		fprintf(fpout, "%d %d %d %d %d %d", lf[0 * 6 + 1].EV1 > 0, lf[1 * 6 + 1].EV1 > 0, lf[2 * 6 + 1].EV1 > 0, lf[3 * 6 + 1].EV1 > 0, lf[4 * 6 + 1].EV1 > 0, lf[4 * 6 + 1].EV2 > 0);
		fflush(fpout);
		fclose(fpout);
	}

	return (0);
}
