#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>

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
    int *read_mat=NULL;
    int* mat_sizes=NULL;
    int N;
    if(world_rank == 0)
    {
        //READ MATRIX INTO A 1D VECTOR
        FILE *file;
        file=fopen("matrix.txt", "r");
        fscanf(file, "%d", &N);

        read_mat=malloc(N*N*sizeof(int*));

        for(i = 0; i < N*N; i++)
        {
            if (!fscanf(file, "%d", &read_mat[i]))
                 break;
        }
        fclose(file);
    }

    //Send N to all processes
    MPI_Bcast( &N, 1, MPI_INT, 0, MPI_COMM_WORLD );

    int* v=malloc(N*sizeof(int*));

    if(world_rank == 0)
    {
        //READ VECTOR
        FILE *file;
        file=fopen("vector.txt", "r");

        for(i = 0; i < N; i++)
        {
            if (!fscanf(file, "%d", &v[i]))
                 break;
        }
        fclose(file);

        int general_chunk_size = N / world_size;
        mat_sizes = (int *)malloc(world_size*sizeof(int *));
        for (int i=0; i<world_size-1; i++)
        {
            mat_sizes[i]=general_chunk_size;
        }
        mat_sizes[world_size-1]=N-(world_size-1)*general_chunk_size;
    }

    //Send v to all processes
    MPI_Bcast( v, N, MPI_INT, 0, MPI_COMM_WORLD );

    //Send chunk size to all processes
    MPI_Scatter( mat_sizes, 1, MPI_INT, &chunk_size, 1, MPI_INT, 0, MPI_COMM_WORLD);

    //printf("Process %d: Chunk size: %d\n", world_rank, chunk_size);

    //Send corresponding lines to all processes
    int *mat_displs = NULL;
    int *snd_counts=NULL;
    if(world_rank == 0)
    {
        mat_displs = (int *)malloc(world_size*sizeof(int));

        mat_displs[0]=0;

        for(i=1; i<world_size; i++)
        {
            mat_displs[i]=mat_displs[i-1]+mat_sizes[i-1]*N;
        }

        snd_counts=malloc(world_size*sizeof(int*));
        for(int i=0; i<world_size; i++)
            snd_counts[i]=N*mat_sizes[i];
    }

    int *mat_1d=malloc(N*chunk_size*sizeof(int*));
    MPI_Scatterv( read_mat, snd_counts, mat_displs, MPI_INT, mat_1d, N*chunk_size, MPI_INT, 0, MPI_COMM_WORLD);

    int **mat=malloc(chunk_size*sizeof(int*));
    for(i=0;i<chunk_size;i++)
        mat[i]=malloc(N*sizeof(int));

    k=0;
    for(int i=0; i<chunk_size; i++)
    {
        for(int j=0; j<N; j++)
        {
            mat[i][j]=mat_1d[k];
            k++;
        }
    }

    //Compute product
    k=0;
    int *res = (int *)malloc(chunk_size*sizeof(int));
    for(i=0; i<chunk_size; i++)
    {
        res[k]=0;
        for(j=0; j<N; j++)
          res[k]+=mat[i][j]*v[j];
        k++;
    }

    //Collect results
    int *final_results=NULL;
    int *final_displs = NULL;
    if(world_rank == 0)
    {
        final_results=(int (*))malloc(sizeof(int)*N);
        final_displs = (int *)malloc(N*sizeof(int));

        final_displs[0]=0;

        for(i=1; i<N; i++)
        {
            final_displs[i]=final_displs[i-1]+mat_sizes[i-1];
        }

    }

    MPI_Gatherv(res, chunk_size, MPI_INT, final_results, mat_sizes, final_displs, MPI_INT, 0, MPI_COMM_WORLD);
    
    //Pprint results to file
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