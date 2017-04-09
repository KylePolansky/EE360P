#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int readVectorFromFile(int ** v, FILE *file);
int * computeProduct(int **mat, int *v, int N, int M);

int main(int argc, char** argv) {
    int i,j,k;
    // Initialize the MPI environment. The two arguments to MPI Init are not
    // currently used by MPI implementations, but are there in case future
    // implementations might need the arguments.
    MPI_Init(NULL, NULL);

    // Get the number of processes
    int world_size;
    MPI_Comm_size(MPI_COMM_WORLD, &world_size);

    // Get the rank of the process
    int world_rank;
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

    int *array_sizes = NULL;
    int chunk_size;
    int *mat_send=NULL;
    int* v=NULL;
    int* lines=NULL;
    int N,M;

    //READ MATRIX AND VECTOR
    if(world_rank == 0)
    {
        FILE *Mfile;
        Mfile=fopen("matrix.txt", "r");
        FILE *Vfile;
        Vfile=fopen("vector.txt", "r");

        fscanf(Mfile, "%d", &N);

        readVectorFromFile(&mat_send, Mfile);
        M=readVectorFromFile(&v,Vfile);

        int general_chunk_size = N / world_size;
        lines = (int *)malloc(world_size*sizeof(int *));
        for (int i=0; i<world_size-1; i++)
        {
            lines[i]=general_chunk_size;
        }
        lines[world_size-1]=N-(world_size-1)*general_chunk_size;
    }
    

    //Send M to all processes
    MPI_Bcast( &M, 1, MPI_INT, 0, MPI_COMM_WORLD );
    if(world_rank != 0)
        v=malloc(M*sizeof(int*));
    
    //Send v to all processes
    MPI_Bcast( v, M, MPI_INT, 0, MPI_COMM_WORLD );

    //Send chunk size to all processes
    MPI_Scatter( lines, 1, MPI_INT, &chunk_size, 1, MPI_INT, 0, MPI_COMM_WORLD);

    //Send corresponding lines to all processes
    int *mat_displacements = NULL;
    int *snd_counts=NULL;
    if(world_rank == 0)
    {
        snd_counts=malloc(world_size*sizeof(int*));
        for(int i=0; i<world_size; i++)
            snd_counts[i]=M*lines[i];

        mat_displacements = (int *)malloc(world_size*sizeof(int));

        mat_displacements[0]=0;
        for(i=1; i<world_size; i++)
            mat_displacements[i]=mat_displacements[i-1]+snd_counts[i-1];
    }

    int *mat_1d=malloc(M*chunk_size*sizeof(int*));
    MPI_Scatterv( mat_send, snd_counts, mat_displacements, MPI_INT, mat_1d, M*chunk_size, MPI_INT, 0, MPI_COMM_WORLD);

    //Rearrange into matrix form
    int **mat=malloc(chunk_size*sizeof(int*));
    for(i=0;i<chunk_size;i++)
        mat[i]=malloc(M*sizeof(int));

    k=0;
    for(int i=0; i<chunk_size; i++)
    {
        for(int j=0; j<M; j++)
        {
            mat[i][j]=mat_1d[k];
            k++;
        }
    }

    //Compute product
    k=0;
    int *res = computeProduct(mat,v,chunk_size,M);

    //Collect results
    int *final_results=NULL;
    int *final_displs = NULL;
    if(world_rank == 0)
    {
        final_results=(int (*))malloc(sizeof(int)*N);
        final_displs = (int *)malloc(world_size*sizeof(int));

        final_displs[0]=0;

        for(i=1; i<world_size; i++)
        {
            final_displs[i]=final_displs[i-1]+lines[i-1];
        }

    }

    MPI_Gatherv(res, chunk_size, MPI_INT, final_results, lines, final_displs, MPI_INT, 0, MPI_COMM_WORLD);
    
    //Print results to file
    if (world_rank==0)
    {
        FILE *file=fopen("result.txt", "wb");
        for(i=0;i<N;i++) {
            fprintf(file,"%d",final_results[i]);
            if(i<N-1)
                fprintf(file," ");
        }
        fclose(file);
    }
    // Finalize the MPI environment. No more MPI calls can be made after this
    MPI_Finalize();
}

int readVectorFromFile(int ** v, FILE *file)
{
    int i=0;

    int *temp=malloc(1000*sizeof(int));

    while(fscanf(file, "%d", &temp[i]) != EOF)
    {
        i++;
    }    
    
    fclose(file);

    (*v)=malloc(i*sizeof(int*));
    memcpy((*v), temp, i * sizeof(int));
    free(temp);

    return i;
}

int * computeProduct(int **mat, int *v, int N, int M)
{
    int i,j;
    int k=0;
    int *res = (int *)malloc(N*sizeof(int));
    for(i=0; i<N; i++)
    {
        res[k]=0;
        for(j=0; j<M; j++)
            res[k]+=mat[i][j]*v[j];
        k++;
    }
    return res;
}

